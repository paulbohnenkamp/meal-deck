package app.mealdeck.dto;

public record MealExtractionResponse(
        String name,
        String description,
        String category,
        Integer caloriesPerServing,
        Integer carbsPerServing,
        Integer proteinPerServing,
        Integer fatPerServing,
        Integer sodiumMgPerServing) {}
