package app.mealdeck.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Records one successfully confirmed provider shipment for idempotency. */
@Entity
@Table(name = "shipments")
public class Shipment {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String externalOrderId;
    private String shippedAt;
    @Column(nullable = false)
    private int totalBoxes;
    @Column(nullable = false)
    private Instant confirmedAt;

    /** Creates an empty JPA entity. */
    public Shipment() {}

    /** Initializes identifier and confirmation timestamp. */
    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (confirmedAt == null) confirmedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getExternalOrderId() { return externalOrderId; }
    public void setExternalOrderId(String value) { this.externalOrderId = value; }
    public String getShippedAt() { return shippedAt; }
    public void setShippedAt(String value) { this.shippedAt = value; }
    public int getTotalBoxes() { return totalBoxes; }
    public void setTotalBoxes(int value) { this.totalBoxes = value; }
    public Instant getConfirmedAt() { return confirmedAt; }
}
