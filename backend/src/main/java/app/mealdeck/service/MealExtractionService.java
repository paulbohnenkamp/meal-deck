package app.mealdeck.service;

import org.springframework.web.multipart.MultipartFile;

import app.mealdeck.dto.MealExtractionResponse;

/** Extracts a reviewable meal draft from a meal-card photo pair. */
public interface MealExtractionService {
    /**
     * @param front meal-card front photo
     * @param back cooking-guide back photo
     * @return extracted visible fields and per-serving nutrition
     */
    MealExtractionResponse extract(MultipartFile front, MultipartFile back);
}
