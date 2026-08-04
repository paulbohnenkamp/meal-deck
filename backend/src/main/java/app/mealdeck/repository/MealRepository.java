package app.mealdeck.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import app.mealdeck.entity.Meal;

/** Provides persistence queries for consolidated freezer inventory. */
public interface MealRepository extends JpaRepository<Meal, UUID> {
    /**
     * @param normalizedName canonical meal-name key
     * @return matching consolidated meal, when present
     */
    Optional<Meal> findByNormalizedName(String normalizedName);

    /**
     * @param cookingMealCode exact provider cooking identifier
     * @return inventory row already associated with that code
     */
    Optional<Meal> findFirstByCookingMealCode(String cookingMealCode);

    /**
     * @param quantity exclusive lower inventory bound
     * @return matching meals ordered by display name
     */
    List<Meal> findByQuantityGreaterThanOrderByNameAsc(int quantity);
}
