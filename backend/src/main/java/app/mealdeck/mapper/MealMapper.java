package app.mealdeck.mapper;

import org.springframework.stereotype.Component;

import app.mealdeck.dto.MealResponse;
import app.mealdeck.entity.Meal;

@Component
public class MealMapper {
    public MealResponse toResponse(Meal meal) {
        return new MealResponse(
                meal.getId(),
                meal.getName(),
                meal.getDescription(),
                meal.getCategory(),
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
