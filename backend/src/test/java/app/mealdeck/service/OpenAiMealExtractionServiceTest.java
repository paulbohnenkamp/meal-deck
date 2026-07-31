package app.mealdeck.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Verifies extraction remains safely disabled without provider credentials. */
class OpenAiMealExtractionServiceTest {
    @Test
    void keepsExtractionUnavailableWithoutAnApiKey() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(mock(ChatClient.class));
        OpenAiMealExtractionService service = new OpenAiMealExtractionService(
                builder, mock(BarcodeDecoder.class), "", "test-model");
        MockMultipartFile image = new MockMultipartFile(
                "front", "meal.jpg", "image/jpeg", new byte[] { 1 });

        assertThatThrownBy(() -> service.extract(image, image))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                })
                .hasMessageContaining("OPENAI_API_KEY");
    }
}
