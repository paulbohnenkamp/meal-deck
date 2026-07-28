package app.mealdeck.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import app.mealdeck.dto.MealExtractionResponse;
import app.mealdeck.service.MealExtractionService;

class MealExtractionControllerTest {
    @Test
    void returnsStructuredPerServingExtractionForThePhotoPair() {
        MealExtractionService service = mock(MealExtractionService.class);
        MealExtractionController controller = new MealExtractionController(service);
        MockMultipartFile front = new MockMultipartFile("front", "front.jpg", "image/jpeg", new byte[] { 1 });
        MockMultipartFile back = new MockMultipartFile("back", "back.jpg", "image/jpeg", new byte[] { 2 });
        MealExtractionResponse expected = new MealExtractionResponse(
                "Teriyaki Salmon", "Salmon with rice", "Seafood", 510, 42, 31, 18, 850);
        when(service.extract(front, back)).thenReturn(expected);

        assertThat(controller.extract(front, back)).isEqualTo(expected);
    }
}
