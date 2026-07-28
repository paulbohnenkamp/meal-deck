package app.mealdeck.dto;

import java.time.Instant;
import java.util.UUID;

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
        String cookingGuideImageUrl,
        String source,
        Instant createdAt,
        Instant updatedAt) {}
