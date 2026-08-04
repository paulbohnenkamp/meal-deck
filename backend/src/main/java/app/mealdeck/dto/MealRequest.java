package app.mealdeck.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Client-supplied values for creating or updating an inventory meal.
 *
 * @param name required display name
 * @param sides printed side dishes without the leading word "with"
 * @param category optional grouping
 * @param cookingMealCode user-confirmed appliance cooking code
 * @param frontBarcodePayload reviewed raw front-barcode payload
 * @param backQrPayload reviewed raw back-QR payload
 * @param quantity number of two-serving boxes
 * @param caloriesPerServing calories per serving
 * @param carbsPerServing carbohydrates in grams per serving
 * @param proteinPerServing protein in grams per serving
 * @param fatPerServing fat in grams per serving
 * @param sodiumMgPerServing sodium in milligrams per serving
 * @param imageUrl front-photo URL
 * @param cookingGuideImageUrl cooking-guide photo URL
 * @param source origin label such as manual, sample, or restored
 */
public record MealRequest(
        @NotBlank String name,
        String sides,
        String category,
        String cookingMealCode,
        String frontBarcodePayload,
        String backQrPayload,
        @Min(0) Integer quantity,
        Integer caloriesPerServing,
        Integer carbsPerServing,
        Integer proteinPerServing,
        Integer fatPerServing,
        Integer sodiumMgPerServing,
        String imageUrl,
        String cookingGuideImageUrl,
        String source) {}
