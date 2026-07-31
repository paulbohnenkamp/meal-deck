package app.mealdeck.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import app.mealdeck.entity.MealTemplate;

/** Persistence access for immutable meal-template revisions. */
public interface MealTemplateRepository extends JpaRepository<MealTemplate, UUID> {
    List<MealTemplate> findByProviderAndActiveTrueAndCookingMealCode(String provider, String value);
    List<MealTemplate> findByProviderAndActiveTrueAndFrontBarcodePayload(String provider, String value);
    List<MealTemplate> findByProviderAndActiveTrueAndBackQrPayload(String provider, String value);
}
