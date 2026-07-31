package app.mealdeck.dto;

import java.util.List;

/**
 * Provider-structured OCR result before local template matching.
 *
 * @param orderId visible shipment order identifier
 * @param shippedAt visible shipment date as printed
 * @param lines visible item-table rows
 */
public record PackingSlipDraft(
        String orderId,
        String shippedAt,
        List<PackingSlipLineDraft> lines) {}
