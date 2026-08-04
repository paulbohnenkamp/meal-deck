package app.mealdeck.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Reusable reviewed meal definition returned by identifier lookup.
 *
 * @param id template revision identifier
 * @param provider source provider
 * @param revision monotonically increasing definition revision
 * @param cookingMealCode appliance cooking code
 * @param frontBarcodePayload raw front-barcode payload
 * @param backQrPayload raw back-QR payload
 * @param name meal name
 * @param sides printed side dishes without the leading word "with"
 * @param category meal grouping
 * @param caloriesPerServing calories per serving
 * @param carbsPerServing carbohydrates per serving
 * @param proteinPerServing protein per serving
 * @param fatPerServing fat per serving
 * @param sodiumMgPerServing sodium per serving
 * @param imageUrl reviewed front image
 * @param cookingGuideImageUrl reviewed back image
 * @param verifiedAt review timestamp
 */
public record MealTemplateResponse(
        UUID id,
        String provider,
        int revision,
        String cookingMealCode,
        String frontBarcodePayload,
        String backQrPayload,
        String name,
        String sides,
        String category,
        Integer caloriesPerServing,
        Integer carbsPerServing,
        Integer proteinPerServing,
        Integer fatPerServing,
        Integer sodiumMgPerServing,
        String imageUrl,
        String cookingGuideImageUrl,
        Instant verifiedAt) {}
