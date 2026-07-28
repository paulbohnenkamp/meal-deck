package app.mealdeck.entity;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "meals")
public class Meal {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String normalizedName;

    private String description;
    private String category;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int servings;

    private Integer caloriesPerServing;
    private Integer carbsPerServing;
    private Integer proteinPerServing;
    private Integer fatPerServing;
    private Integer sodiumMgPerServing;
    private String imageUrl;
    private String cookingGuideImageUrl;
    private String source;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Meal() {}

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        updatedAt = Instant.now();
        normalizedName = normalize(name);
        if (servings <= 0) servings = 2;
        if (source == null || source.isBlank()) source = "MANUAL";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        normalizedName = normalize(name);
    }

    public static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(String normalizedName) { this.normalizedName = normalizedName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getServings() { return servings; }
    public void setServings(int servings) { this.servings = servings; }
    public Integer getCaloriesPerServing() { return caloriesPerServing; }
    public void setCaloriesPerServing(Integer caloriesPerServing) { this.caloriesPerServing = caloriesPerServing; }
    public Integer getCarbsPerServing() { return carbsPerServing; }
    public void setCarbsPerServing(Integer carbsPerServing) { this.carbsPerServing = carbsPerServing; }
    public Integer getProteinPerServing() { return proteinPerServing; }
    public void setProteinPerServing(Integer proteinPerServing) { this.proteinPerServing = proteinPerServing; }
    public Integer getFatPerServing() { return fatPerServing; }
    public void setFatPerServing(Integer fatPerServing) { this.fatPerServing = fatPerServing; }
    public Integer getSodiumMgPerServing() { return sodiumMgPerServing; }
    public void setSodiumMgPerServing(Integer sodiumMgPerServing) { this.sodiumMgPerServing = sodiumMgPerServing; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCookingGuideImageUrl() { return cookingGuideImageUrl; }
    public void setCookingGuideImageUrl(String cookingGuideImageUrl) { this.cookingGuideImageUrl = cookingGuideImageUrl; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
