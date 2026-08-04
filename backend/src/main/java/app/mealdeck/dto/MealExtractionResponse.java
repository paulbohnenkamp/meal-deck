package app.mealdeck.dto;

/**
 * Reviewable fields extracted from meal-card photos.
 *
 * @param name visible meal name
 * @param sides printed side dishes without the leading word "with"
 * @param category useful meal grouping
 * @param frontCookingMealCode cooking code printed on the front card
 * @param backCookingMealCode cooking code printed on the cooking-guide back
 * @param cookingMealCode proposed code when the visible candidates agree or
 *        only one candidate is readable
 * @param frontBarcodePayload raw payload decoded from a one-dimensional barcode
 *        on the front card
 * @param backQrPayload raw payload decoded from a QR code on the back card
 * @param caloriesPerServing calories per serving
 * @param carbsPerServing carbohydrates in grams per serving
 * @param proteinPerServing protein in grams per serving
 * @param fatPerServing fat in grams per serving
 * @param sodiumMgPerServing sodium in milligrams per serving
 */
public record MealExtractionResponse(
        String name,
        String sides,
        String category,
        String frontCookingMealCode,
        String backCookingMealCode,
        String cookingMealCode,
        String frontBarcodePayload,
        String backQrPayload,
        Integer caloriesPerServing,
        Integer carbsPerServing,
        Integer proteinPerServing,
        Integer fatPerServing,
        Integer sodiumMgPerServing) {}
