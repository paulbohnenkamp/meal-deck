package app.mealdeck.dto;

import java.time.Instant;
import java.util.List;

/**
 * Result of one atomic shipment confirmation.
 *
 * @param orderId confirmed provider order identifier
 * @param totalBoxes total boxes added
 * @param meals resulting consolidated inventory rows
 * @param confirmedAt transaction completion time
 */
public record ShipmentConfirmationResponse(
        String orderId,
        int totalBoxes,
        List<MealResponse> meals,
        Instant confirmedAt) {}
