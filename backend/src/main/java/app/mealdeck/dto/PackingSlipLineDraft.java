package app.mealdeck.dto;

/**
 * One visible packing-slip item-table row.
 *
 * @param itemCode provider item code
 * @param quantity shipped box quantity
 * @param description printed meal description
 */
public record PackingSlipLineDraft(
        String itemCode,
        Integer quantity,
        String description) {}
