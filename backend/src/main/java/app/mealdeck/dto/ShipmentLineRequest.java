package app.mealdeck.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * One resolved shipment line selected for atomic confirmation.
 *
 * @param templateId active reviewed template revision
 * @param itemCode packing-slip item code retained for verification
 * @param quantity number of two-serving boxes
 */
public record ShipmentLineRequest(
        @NotNull UUID templateId,
        String itemCode,
        @Min(1) int quantity) {}
