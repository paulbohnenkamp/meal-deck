package app.mealdeck.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

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
        String cookingGuideImageUrl,
        String source) {}
