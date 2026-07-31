package app.mealdeck.dto;

/**
 * Reviewable packing-slip line enriched by local template matching.
 *
 * @param itemCode provider item code
 * @param quantity shipped box quantity
 * @param description printed meal description
 * @param status {@code KNOWN}, {@code CHANGED}, or {@code UNKNOWN}
 * @param template matched active template, when present
 */
public record PackingSlipLineResponse(
        String itemCode,
        int quantity,
        String description,
        String status,
        MealTemplateResponse template) {}
