package app.mealdeck.dto;

import java.time.Instant;
import java.util.UUID;

public record HistoryResponse(
        UUID id,
        UUID mealId,
        String mealName,
        Integer carbsPerServing,
        int servings,
        Instant consumedAt) {}
