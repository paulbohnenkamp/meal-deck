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
public class MealExtractionController {
    private final MealExtractionService extractionService;

    public MealExtractionController(MealExtractionService extractionService) {
        this.extractionService = extractionService;
    }

    @PostMapping
    public MealExtractionResponse extract(
            @RequestPart("front") MultipartFile front,
            @RequestPart("back") MultipartFile back) {
        return extractionService.extract(front, back);
    }
}
