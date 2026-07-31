package app.mealdeck.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import app.mealdeck.entity.Shipment;

/** Persistence access for confirmed shipment idempotency records. */
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    boolean existsByExternalOrderId(String externalOrderId);
}
