package app.mealdeck.api;

import java.time.Instant;
import java.util.UUID;

import app.mealdeck.history.MealHistory;
import app.mealdeck.meal.Meal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public final class MealDtos {
    private MealDtos() {}

    public record MealRequest(
            @NotBlank String name,
            String description,
            String category,
            @Min(0) Integer quantity,
            Integer caloriesPerServing,
            Integer carbsPerServing,
            Integer proteinPerServing,
            Integer fatPerServing,
            Integer sodiumMgPerServing,
            String imageUrl,
            String source) {}

    public record MealResponse(
            UUID id,
            String name,
            String description,
            String category,
            int quantity,
            int servings,
            Integer caloriesPerServing,
            Integer carbsPerServing,
            Integer proteinPerServing,
            Integer fatPerServing,
            Integer sodiumMgPerServing,
            String imageUrl,
            String source,
            Instant createdAt,
            Instant updatedAt) {
        public static MealResponse from(Meal meal) {
            return new MealResponse(meal.getId(), meal.getName(), meal.getDescription(), meal.getCategory(),
                    meal.getQuantity(), meal.getServings(), meal.getCaloriesPerServing(), meal.getCarbsPerServing(),
                    meal.getProteinPerServing(), meal.getFatPerServing(), meal.getSodiumMgPerServing(),
                    meal.getImageUrl(), meal.getSource(), meal.getCreatedAt(), meal.getUpdatedAt());
        }
    }

    public record HistoryResponse(
            UUID id,
            UUID mealId,
            String mealName,
            Integer carbsPerServing,
            int servings,
            Instant consumedAt) {
        public static HistoryResponse from(MealHistory history) {
            return new HistoryResponse(history.getId(), history.getMealId(), history.getMealName(),
                    history.getCarbsPerServingSnapshot(), history.getServingsSnapshot(), history.getConsumedAt());
        }
    }

    public record PickResponse(MealResponse meal, HistoryResponse history, int avoidDays) {}
    public record DashboardResponse(long mealTypes, int totalBoxes, long eligibleMealTypes, int avoidDays) {}
    public record UploadResponse(String imageUrl) {}
}
