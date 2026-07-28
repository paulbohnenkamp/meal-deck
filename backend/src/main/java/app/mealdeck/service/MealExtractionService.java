package app.mealdeck.service;

import org.springframework.web.multipart.MultipartFile;

import app.mealdeck.dto.MealExtractionResponse;

public interface MealExtractionService {
    MealExtractionResponse extract(MultipartFile front, MultipartFile back);
}
