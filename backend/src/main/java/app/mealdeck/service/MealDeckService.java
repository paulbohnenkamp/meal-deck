package app.mealdeck.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import app.mealdeck.api.MealDtos.DashboardResponse;
import app.mealdeck.api.MealDtos.HistoryResponse;
import app.mealdeck.api.MealDtos.MealRequest;
import app.mealdeck.api.MealDtos.MealResponse;
import app.mealdeck.api.MealDtos.PickResponse;
import app.mealdeck.history.MealHistory;
import app.mealdeck.history.MealHistoryRepository;
import app.mealdeck.meal.Meal;
import app.mealdeck.meal.MealRepository;

@Service
public class MealDeckService {
    private final MealRepository meals;
    private final MealHistoryRepository history;

    public MealDeckService(MealRepository meals, MealHistoryRepository history) {
        this.meals = meals;
        this.history = history;
    }

    @Transactional(readOnly = true)
    public List<MealResponse> listMeals() {
        return meals.findAll().stream()
                .sorted(Comparator.comparing(Meal::getName, String.CASE_INSENSITIVE_ORDER))
                .map(MealResponse::from).toList();
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
        meal.setSource(request.source());
        return MealResponse.from(meals.save(meal));
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
        meal.setSource(request.source());
        try {
            return MealResponse.from(meals.saveAndFlush(meal));
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another meal already uses that name", ex);
        }
    }

    @Transactional
    public void deleteMeal(UUID id) {
        if (!meals.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Meal not found");
        meals.deleteById(id);
    }

    @Transactional
    public PickResponse pickRandom(int avoidDays, boolean allowRecent) {
        List<Meal> available = meals.findByQuantityGreaterThanOrderByNameAsc(0);
        if (available.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Your freezer inventory is empty");

        Set<String> recentlyEaten = recentNames(avoidDays);
        List<Meal> eligible = available.stream()
                .filter(meal -> allowRecent || !recentlyEaten.contains(meal.getNormalizedName()))
                .toList();

        if (eligible.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Every available meal was eaten in the last " + avoidDays + " days. Try the relaxed draw.");
        }

        Meal selected = eligible.get(ThreadLocalRandom.current().nextInt(eligible.size()));
        return consume(selected, avoidDays);
    }

    @Transactional
    public PickResponse consume(UUID mealId, int avoidDays) {
        return consume(getMeal(mealId), avoidDays);
    }

    private PickResponse consume(Meal meal, int avoidDays) {
        if (meal.getQuantity() <= 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "That meal is out of stock");
        meal.setQuantity(meal.getQuantity() - 1);
        meals.save(meal);

        MealHistory entry = new MealHistory();
        entry.setMealId(meal.getId());
        entry.setMealName(meal.getName());
        entry.setNormalizedMealName(meal.getNormalizedName());
        entry.setCarbsPerServingSnapshot(meal.getCarbsPerServing());
        entry.setServingsSnapshot(meal.getServings());
        history.save(entry);
        return new PickResponse(MealResponse.from(meal), HistoryResponse.from(entry), avoidDays);
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> listHistory() {
        return history.findAllByOrderByConsumedAtDesc().stream().map(HistoryResponse::from).toList();
    }

    @Transactional
    public MealResponse undoHistory(UUID historyId) {
        MealHistory entry = history.findById(historyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "History entry not found"));
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
        return MealResponse.from(saved);
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
        return meals.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meal not found"));
    }

    private int valueOr(Integer value, int fallback) { return value == null ? fallback : value; }
}
