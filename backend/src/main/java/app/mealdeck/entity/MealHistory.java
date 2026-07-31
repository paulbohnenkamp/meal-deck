package app.mealdeck.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "meal_history")
/**
 * Persistent snapshot of a consumed meal.
 *
 * <p>The name, servings, and carbohydrate values remain unchanged if the
 * corresponding inventory meal is later edited.</p>
 */
public class MealHistory {
    @Id
    private UUID id;

    private UUID mealId;

    @Column(nullable = false)
    private String mealName;

    @Column(nullable = false)
    private String normalizedMealName;

    private Integer carbsPerServingSnapshot;

    @Column(nullable = false)
    private int servingsSnapshot;

    @Column(nullable = false)
    private Instant consumedAt;

    /** Creates an empty JPA entity. */
    public MealHistory() {}

    /** Initializes the identifier and consumption timestamp when absent. */
    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (consumedAt == null) consumedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMealId() { return mealId; }
    public void setMealId(UUID mealId) { this.mealId = mealId; }
    public String getMealName() { return mealName; }
    public void setMealName(String mealName) { this.mealName = mealName; }
    public String getNormalizedMealName() { return normalizedMealName; }
    public void setNormalizedMealName(String normalizedMealName) { this.normalizedMealName = normalizedMealName; }
    public Integer getCarbsPerServingSnapshot() { return carbsPerServingSnapshot; }
    public void setCarbsPerServingSnapshot(Integer carbsPerServingSnapshot) { this.carbsPerServingSnapshot = carbsPerServingSnapshot; }
    public int getServingsSnapshot() { return servingsSnapshot; }
    public void setServingsSnapshot(int servingsSnapshot) { this.servingsSnapshot = servingsSnapshot; }
    public Instant getConsumedAt() { return consumedAt; }
    public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }
}
