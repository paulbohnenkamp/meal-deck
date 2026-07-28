package app.mealdeck.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import app.mealdeck.entity.Meal;

public interface MealRepository extends JpaRepository<Meal, UUID> {
    Optional<Meal> findByNormalizedName(String normalizedName);
    List<Meal> findByQuantityGreaterThanOrderByNameAsc(int quantity);
}
