package app.mealdeck.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Fully reviewed shipment submitted for all-or-nothing confirmation.
 *
 * @param orderId required provider order identifier
 * @param shippedAt optional printed shipment date
 * @param lines resolved template-backed shipment rows
 */
public record ShipmentConfirmationRequest(
        @NotBlank String orderId,
        String shippedAt,
        @NotEmpty List<@Valid ShipmentLineRequest> lines) {}
