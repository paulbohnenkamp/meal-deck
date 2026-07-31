package app.mealdeck.dto;

import java.util.List;

/**
 * Reviewable shipment manifest extracted from one packing-slip photo.
 *
 * @param orderId visible shipment order identifier
 * @param shippedAt visible shipment date as printed
 * @param lines classified shipment rows
 */
public record PackingSlipResponse(
        String orderId,
        String shippedAt,
        List<PackingSlipLineResponse> lines) {}
