package app.mealdeck.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.mealdeck.dto.DashboardResponse;
import app.mealdeck.dto.HistoryResponse;
import app.mealdeck.dto.MealRequest;
import app.mealdeck.dto.MealResponse;
import app.mealdeck.dto.PickResponse;
import app.mealdeck.entity.Meal;
import app.mealdeck.entity.MealHistory;
import app.mealdeck.exception.DuplicateMealNameException;
import app.mealdeck.exception.HistoryNotFoundException;
import app.mealdeck.exception.InventoryConflictException;
import app.mealdeck.exception.MealNotFoundException;
import app.mealdeck.mapper.HistoryMapper;
import app.mealdeck.mapper.MealMapper;
import app.mealdeck.repository.MealHistoryRepository;
import app.mealdeck.repository.MealRepository;

@Service
/**
 * Owns transactional inventory, random-selection, and history rules.
 */
public class MealDeckService {
    private final MealRepository meals;
    private final MealHistoryRepository history;
    private final MealMapper mealMapper;
    private final HistoryMapper historyMapper;
    private final MealTemplateService templates;

    /**
     * Creates the domain service with its persistence and mapping collaborators.
     *
     * @param meals inventory repository
     * @param history dinner-history repository
     * @param mealMapper inventory response mapper
     * @param historyMapper history response mapper
     * @param templates reusable reviewed meal definitions
     */
    public MealDeckService(MealRepository meals, MealHistoryRepository history,
                           MealMapper mealMapper, HistoryMapper historyMapper,
                           MealTemplateService templates) {
        this.meals = meals;
        this.history = history;
        this.mealMapper = mealMapper;
        this.historyMapper = historyMapper;
        this.templates = templates;
    }

    /** @return all inventory meals sorted case-insensitively by name */
    @Transactional(readOnly = true)
    public List<MealResponse> listMeals() {
        return meals.findAll().stream()
                .sorted(Comparator.comparing(Meal::getName, String.CASE_INSENSITIVE_ORDER))
                .map(mealMapper::toResponse).toList();
    }

    /**
     * Adds boxes, matching an exact cooking code first and otherwise
     * consolidating duplicates by normalized meal name.
     *
     * @param request new meal values
     * @return saved consolidated inventory meal
     */
    @Transactional
    public MealResponse addMeal(MealRequest request) {
        String normalized = Meal.normalize(request.name());
        String cookingCode = cleanCode(request.cookingMealCode());
        Meal meal = (cookingCode == null
                ? meals.findByNormalizedName(normalized)
                : meals.findFirstByCookingMealCode(cookingCode)
                        .or(() -> meals.findByNormalizedName(normalized)))
                .orElseGet(Meal::new);
        boolean existing = meal.getId() != null;
        meal.setName(request.name().trim());
        meal.setSides(request.sides());
        meal.setCategory(request.category());
        meal.setCookingMealCode(cookingCode);
        meal.setFrontBarcodePayload(cleanCode(request.frontBarcodePayload()));
        meal.setBackQrPayload(cleanCode(request.backQrPayload()));
        meal.setQuantity((existing ? meal.getQuantity() : 0) + valueOr(request.quantity(), 1));
        meal.setServings(2);
        meal.setCaloriesPerServing(request.caloriesPerServing());
        meal.setCarbsPerServing(request.carbsPerServing());
        meal.setProteinPerServing(request.proteinPerServing());
        meal.setFatPerServing(request.fatPerServing());
        meal.setSodiumMgPerServing(request.sodiumMgPerServing());
        meal.setImageUrl(request.imageUrl());
        meal.setCookingGuideImageUrl(request.cookingGuideImageUrl());
        meal.setSource(request.source());
        MealResponse response = mealMapper.toResponse(meals.save(meal));
        templates.remember(request);
        return response;
    }

    /**
     * Updates an existing meal while preserving its two-serving invariant.
     *
     * @param id meal identifier
     * @param request replacement values
     * @return updated inventory meal
     */
    @Transactional
    public MealResponse updateMeal(UUID id, MealRequest request) {
        Meal meal = getMeal(id);
        if (!java.util.Objects.equals(
                cleanCode(request.cookingMealCode()), meal.getCookingMealCode())) {
            throw new InventoryConflictException(
                    "A confirmed cooking meal code cannot be changed");
        }
        meal.setName(request.name().trim());
        meal.setSides(request.sides());
        meal.setCategory(request.category());
        // A confirmed cooking code is the stable provider identity. Corrections
        // happen during extraction review before the inventory row is created.
        meal.setFrontBarcodePayload(cleanCode(request.frontBarcodePayload()));
        meal.setBackQrPayload(cleanCode(request.backQrPayload()));
        meal.setQuantity(valueOr(request.quantity(), meal.getQuantity()));
        meal.setCaloriesPerServing(request.caloriesPerServing());
        meal.setCarbsPerServing(request.carbsPerServing());
        meal.setProteinPerServing(request.proteinPerServing());
        meal.setFatPerServing(request.fatPerServing());
        meal.setSodiumMgPerServing(request.sodiumMgPerServing());
        meal.setImageUrl(request.imageUrl());
        meal.setCookingGuideImageUrl(request.cookingGuideImageUrl());
        meal.setSource(request.source());
        try {
            MealResponse response = mealMapper.toResponse(meals.saveAndFlush(meal));
            templates.remember(request);
            return response;
        } catch (RuntimeException ex) {
            throw new DuplicateMealNameException(ex);
        }
    }

    /** @param id identifier of the inventory meal to delete */
    @Transactional
    public void deleteMeal(UUID id) {
        if (!meals.existsById(id)) throw new MealNotFoundException(id);
        meals.deleteById(id);
    }

    /**
     * Selects, consumes, and records an eligible random meal atomically.
     *
     * @param avoidDays recent-history exclusion window
     * @param allowRecent whether to relax the recent-name exclusion
     * @return consumed meal and its history snapshot
     */
    @Transactional
    public PickResponse pickRandom(int avoidDays, boolean allowRecent) {
        MealResponse preview = previewRandom(avoidDays, allowRecent, null);
        return consume(preview.id(), avoidDays);
    }

    /**
     * Selects an eligible meal without changing inventory or history.
     *
     * @param avoidDays recent-history exclusion window
     * @param allowRecent whether to relax the recent-name exclusion
     * @param excludeMealId previous preview omitted when alternatives exist
     * @return selected inventory meal
     */
    @Transactional(readOnly = true)
    public MealResponse previewRandom(int avoidDays, boolean allowRecent, UUID excludeMealId) {
        List<Meal> available = meals.findByQuantityGreaterThanOrderByNameAsc(0);
        if (available.isEmpty()) throw new InventoryConflictException("Your freezer inventory is empty");

        Set<String> recentlyEaten = recentNames(avoidDays);
        List<Meal> eligible = available.stream()
                .filter(meal -> allowRecent || !recentlyEaten.contains(meal.getNormalizedName()))
                .toList();

        if (eligible.isEmpty()) {
            throw new InventoryConflictException(
                    "Every available meal was eaten in the last " + avoidDays + " days. Try the relaxed draw.");
        }

        List<Meal> choices = eligible.size() > 1 && excludeMealId != null
                ? eligible.stream().filter(meal -> !meal.getId().equals(excludeMealId)).toList()
                : eligible;
        Meal selected = choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
        return mealMapper.toResponse(selected);
    }

    /**
     * Consumes one box and records a nutrition snapshot atomically.
     *
     * @param mealId meal identifier
     * @param avoidDays selection window reported in the result
     * @return consumed meal and new history entry
     */
    @Transactional
    public PickResponse consume(UUID mealId, int avoidDays) {
        return consume(getMeal(mealId), avoidDays);
    }

    private PickResponse consume(Meal meal, int avoidDays) {
        if (meal.getQuantity() <= 0) throw new InventoryConflictException("That meal is out of stock");
        meal.setQuantity(meal.getQuantity() - 1);
        meals.save(meal);

        MealHistory entry = new MealHistory();
        entry.setMealId(meal.getId());
        entry.setMealName(meal.getName());
        entry.setNormalizedMealName(meal.getNormalizedName());
        entry.setCarbsPerServingSnapshot(meal.getCarbsPerServing());
        entry.setServingsSnapshot(meal.getServings());
        history.save(entry);
        return new PickResponse(mealMapper.toResponse(meal), historyMapper.toResponse(entry), avoidDays);
    }

    /** @return dinner history newest first */
    @Transactional(readOnly = true)
    public List<HistoryResponse> listHistory() {
        return history.findAllByOrderByConsumedAtDesc().stream().map(historyMapper::toResponse).toList();
    }

    /**
     * Deletes a history entry and restores exactly one meal box.
     *
     * @param historyId history identifier
     * @return restored inventory meal
     */
    @Transactional
    public MealResponse undoHistory(UUID historyId) {
        MealHistory entry = history.findById(historyId)
                .orElseThrow(() -> new HistoryNotFoundException(historyId));
        Meal meal = entry.getMealId() == null ? null : meals.findById(entry.getMealId()).orElse(null);
        if (meal == null) meal = meals.findByNormalizedName(entry.getNormalizedMealName()).orElse(null);
        if (meal == null) {
            meal = new Meal();
            meal.setName(entry.getMealName());
            meal.setQuantity(0);
            meal.setServings(entry.getServingsSnapshot());
            meal.setCarbsPerServing(entry.getCarbsPerServingSnapshot());
            meal.setSource("RESTORED");
        }
        meal.setQuantity(meal.getQuantity() + 1);
        Meal saved = meals.save(meal);
        history.delete(entry);
        return mealMapper.toResponse(saved);
    }

    /**
     * Computes inventory and strict-draw eligibility totals.
     *
     * @param avoidDays recent-history exclusion window
     * @return dashboard summary
     */
    @Transactional(readOnly = true)
    public DashboardResponse dashboard(int avoidDays) {
        List<Meal> all = meals.findAll();
        Set<String> recent = recentNames(avoidDays);
        int boxes = all.stream().mapToInt(Meal::getQuantity).sum();
        long stockedTypes = all.stream().filter(m -> m.getQuantity() > 0).count();
        long eligible = all.stream().filter(m -> m.getQuantity() > 0 && !recent.contains(m.getNormalizedName())).count();
        return new DashboardResponse(stockedTypes, boxes, eligible, avoidDays);
    }

    private Set<String> recentNames(int avoidDays) {
        Instant since = Instant.now().minus(Math.max(0, avoidDays), ChronoUnit.DAYS);
        return new HashSet<>(history.findByConsumedAtAfterOrderByConsumedAtDesc(since).stream()
                .map(MealHistory::getNormalizedMealName).toList());
    }

    private Meal getMeal(UUID id) {
        return meals.findById(id).orElseThrow(() -> new MealNotFoundException(id));
    }

    private int valueOr(Integer value, int fallback) { return value == null ? fallback : value; }

    private String cleanCode(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
