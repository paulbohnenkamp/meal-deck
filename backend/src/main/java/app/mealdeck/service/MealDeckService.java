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
public class MealDeckService {
    private final MealRepository meals;
    private final MealHistoryRepository history;
    private final MealMapper mealMapper;
    private final HistoryMapper historyMapper;

    public MealDeckService(MealRepository meals, MealHistoryRepository history,
                           MealMapper mealMapper, HistoryMapper historyMapper) {
        this.meals = meals;
        this.history = history;
        this.mealMapper = mealMapper;
        this.historyMapper = historyMapper;
    }

    @Transactional(readOnly = true)
    public List<MealResponse> listMeals() {
        return meals.findAll().stream()
                .sorted(Comparator.comparing(Meal::getName, String.CASE_INSENSITIVE_ORDER))
                .map(mealMapper::toResponse).toList();
    }

    @Transactional
    public MealResponse addMeal(MealRequest request) {
        String normalized = Meal.normalize(request.name());
        Meal meal = meals.findByNormalizedName(normalized).orElseGet(Meal::new);
        boolean existing = meal.getId() != null;
        meal.setName(request.name().trim());
        meal.setDescription(request.description());
        meal.setCategory(request.category());
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
        return mealMapper.toResponse(meals.save(meal));
    }

    @Transactional
    public MealResponse updateMeal(UUID id, MealRequest request) {
        Meal meal = getMeal(id);
        meal.setName(request.name().trim());
        meal.setDescription(request.description());
        meal.setCategory(request.category());
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
            return mealMapper.toResponse(meals.saveAndFlush(meal));
        } catch (RuntimeException ex) {
            throw new DuplicateMealNameException(ex);
        }
    }

    @Transactional
    public void deleteMeal(UUID id) {
        if (!meals.existsById(id)) throw new MealNotFoundException(id);
        meals.deleteById(id);
    }

    @Transactional
    public PickResponse pickRandom(int avoidDays, boolean allowRecent) {
        MealResponse preview = previewRandom(avoidDays, allowRecent, null);
        return consume(preview.id(), avoidDays);
    }

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

    @Transactional(readOnly = true)
    public List<HistoryResponse> listHistory() {
        return history.findAllByOrderByConsumedAtDesc().stream().map(historyMapper::toResponse).toList();
    }

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

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(int avoidDays) {
        List<Meal> all = meals.findAll();
        Set<String> recent = recentNames(avoidDays);
        int boxes = all.stream().mapToInt(Meal::getQuantity).sum();
        long eligible = all.stream().filter(m -> m.getQuantity() > 0 && !recent.contains(m.getNormalizedName())).count();
        return new DashboardResponse(all.size(), boxes, eligible, avoidDays);
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
}
