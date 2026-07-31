package app.mealdeck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Verifies packing-slip extraction remains disabled without credentials. */
class PackingSlipExtractionServiceTest {
    @Test
    void rejectsExtractionWhenNoApiKeyIsConfigured() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(mock(ChatClient.class));
        PackingSlipExtractionService service = new PackingSlipExtractionService(
                builder, mock(PackingSlipMatcher.class), "", "test-model");

        assertThatThrownBy(() -> service.extract(new MockMultipartFile(
                "slip", "slip.jpg", "image/jpeg", new byte[] { 1 })))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE))
                .hasMessageContaining("OPENAI_API_KEY");
    }
}
