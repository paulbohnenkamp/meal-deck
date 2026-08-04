package app.mealdeck.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Complete inventory meal representation returned to clients.
 *
 * @param id meal identifier
 * @param name display name
 * @param sides printed side dishes without the leading word "with"
 * @param category meal grouping
 * @param cookingMealCode appliance cooking code
 * @param frontBarcodePayload reviewed raw front-barcode payload
 * @param backQrPayload reviewed raw back-QR payload
 * @param quantity available two-serving boxes
 * @param servings servings per box
 * @param caloriesPerServing calories per serving
 * @param carbsPerServing carbohydrates in grams per serving
 * @param proteinPerServing protein in grams per serving
 * @param fatPerServing fat in grams per serving
 * @param sodiumMgPerServing sodium in milligrams per serving
 * @param imageUrl front-photo URL
 * @param cookingGuideImageUrl cooking-guide photo URL
 * @param source inventory origin label
 * @param createdAt creation timestamp
 * @param updatedAt last-update timestamp
 */
public record MealResponse(
        UUID id,
        String name,
        String sides,
        String category,
        String cookingMealCode,
        String frontBarcodePayload,
        String backQrPayload,
        int quantity,
        int servings,
        Integer caloriesPerServing,
        Integer carbsPerServing,
        Integer proteinPerServing,
        Integer fatPerServing,
        Integer sodiumMgPerServing,
        String imageUrl,
        String cookingGuideImageUrl,
        String source,
        Instant createdAt,
        Instant updatedAt) {}
