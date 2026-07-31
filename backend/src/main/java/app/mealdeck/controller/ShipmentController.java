package app.mealdeck.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import app.mealdeck.dto.ShipmentConfirmationRequest;
import app.mealdeck.dto.ShipmentConfirmationResponse;
import app.mealdeck.service.ShipmentService;
import jakarta.validation.Valid;

/** Exposes all-or-nothing reviewed shipment confirmation. */
@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {
    private final ShipmentService shipments;

    /**
     * @param shipments transactional shipment service
     */
    public ShipmentController(ShipmentService shipments) {
        this.shipments = shipments;
    }

    /**
     * @param request fully resolved reviewed manifest
     * @return resulting inventory changes
     */
    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentConfirmationResponse confirm(
            @Valid @RequestBody ShipmentConfirmationRequest request) {
        return shipments.confirm(request);
    }
}
