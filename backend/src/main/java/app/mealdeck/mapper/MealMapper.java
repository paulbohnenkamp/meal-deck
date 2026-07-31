package app.mealdeck.mapper;

import org.springframework.stereotype.Component;

import app.mealdeck.dto.MealResponse;
import app.mealdeck.entity.Meal;

@Component
/** Maps inventory entities to immutable API responses. */
public class MealMapper {
    /**
     * @param meal persisted inventory meal
     * @return client-facing meal representation
     */
    public MealResponse toResponse(Meal meal) {
        return new MealResponse(
                meal.getId(),
                meal.getName(),
                meal.getDescription(),
                meal.getCategory(),
                meal.getCookingMealCode(),
                meal.getFrontBarcodePayload(),
                meal.getBackQrPayload(),
                meal.getQuantity(),
                meal.getServings(),
                meal.getCaloriesPerServing(),
                meal.getCarbsPerServing(),
                meal.getProteinPerServing(),
                meal.getFatPerServing(),
                meal.getSodiumMgPerServing(),
                meal.getImageUrl(),
                meal.getCookingGuideImageUrl(),
                meal.getSource(),
                meal.getCreatedAt(),
                meal.getUpdatedAt());
    }
}
