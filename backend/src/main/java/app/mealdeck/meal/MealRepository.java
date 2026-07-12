package app.mealdeck.meal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MealRepository extends JpaRepository<Meal, UUID> {
    Optional<Meal> findByNormalizedName(String normalizedName);
    List<Meal> findByQuantityGreaterThanOrderByNameAsc(int quantity);
}
