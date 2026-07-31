package app.mealdeck.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable dinner-history representation returned to clients.
 *
 * @param id history entry identifier
 * @param mealId original meal identifier, when still known
 * @param mealName meal name captured at consumption time
 * @param carbsPerServing carbohydrate snapshot per serving
 * @param servings servings represented by the consumed box
 * @param consumedAt consumption timestamp
 */
public record HistoryResponse(
        UUID id,
        UUID mealId,
        String mealName,
        Integer carbsPerServing,
        int servings,
        Instant consumedAt) {}
