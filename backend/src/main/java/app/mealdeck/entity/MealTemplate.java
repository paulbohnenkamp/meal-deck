package app.mealdeck.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Immutable, revisioned definition of a previously reviewed provider meal.
 */
@Entity
@Table(name = "meal_templates")
public class MealTemplate {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String provider;
    @Column(nullable = false)
    private int revision;
    @Column(nullable = false)
    private boolean active;
    private String cookingMealCode;
    @Column(length = 512)
    private String frontBarcodePayload;
    @Column(length = 2048)
    private String backQrPayload;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String normalizedName;
    @Column(name = "description")
    private String sides;
    private String category;
    private Integer caloriesPerServing;
    private Integer carbsPerServing;
    private Integer proteinPerServing;
    private Integer fatPerServing;
    private Integer sodiumMgPerServing;
    private String imageUrl;
    private String cookingGuideImageUrl;
    @Column(nullable = false)
    private Instant verifiedAt;

    /** Creates an empty JPA entity. */
    public MealTemplate() {}

    /** Initializes the template identifier and verification timestamp. */
    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (verifiedAt == null) verifiedAt = Instant.now();
        normalizedName = Meal.normalize(name);
    }

    public UUID getId() { return id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public int getRevision() { return revision; }
    public void setRevision(int revision) { this.revision = revision; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getCookingMealCode() { return cookingMealCode; }
    public void setCookingMealCode(String cookingMealCode) { this.cookingMealCode = cookingMealCode; }
    public String getFrontBarcodePayload() { return frontBarcodePayload; }
    public void setFrontBarcodePayload(String value) { this.frontBarcodePayload = value; }
    public String getBackQrPayload() { return backQrPayload; }
    public void setBackQrPayload(String value) { this.backQrPayload = value; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNormalizedName() { return normalizedName; }
    public String getSides() { return sides; }
    public void setSides(String sides) { this.sides = sides; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getCaloriesPerServing() { return caloriesPerServing; }
    public void setCaloriesPerServing(Integer value) { this.caloriesPerServing = value; }
    public Integer getCarbsPerServing() { return carbsPerServing; }
    public void setCarbsPerServing(Integer value) { this.carbsPerServing = value; }
    public Integer getProteinPerServing() { return proteinPerServing; }
    public void setProteinPerServing(Integer value) { this.proteinPerServing = value; }
    public Integer getFatPerServing() { return fatPerServing; }
    public void setFatPerServing(Integer value) { this.fatPerServing = value; }
    public Integer getSodiumMgPerServing() { return sodiumMgPerServing; }
    public void setSodiumMgPerServing(Integer value) { this.sodiumMgPerServing = value; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCookingGuideImageUrl() { return cookingGuideImageUrl; }
    public void setCookingGuideImageUrl(String value) { this.cookingGuideImageUrl = value; }
    public Instant getVerifiedAt() { return verifiedAt; }
}
