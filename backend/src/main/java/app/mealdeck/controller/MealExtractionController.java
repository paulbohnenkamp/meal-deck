package app.mealdeck.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import app.mealdeck.dto.MealExtractionResponse;
import app.mealdeck.service.MealExtractionService;

@RestController
@RequestMapping("/api/extractions")
/**
 * Exposes asynchronous-ready meal-card photo extraction over HTTP.
 */
public class MealExtractionController {
    private final MealExtractionService extractionService;

    /**
     * Creates an extraction controller.
     *
     * @param extractionService service that reads the submitted photo pair
     */
    public MealExtractionController(MealExtractionService extractionService) {
        this.extractionService = extractionService;
    }

    @PostMapping
    /**
     * Extracts a reviewable meal draft from front and back card photos.
     *
     * @param front meal-card front photo
     * @param back cooking-guide back photo
     * @return extracted meal and per-serving nutrition fields
     */
    public MealExtractionResponse extract(
            @RequestPart("front") MultipartFile front,
            @RequestPart("back") MultipartFile back) {
        return extractionService.extract(front, back);
    }
}
