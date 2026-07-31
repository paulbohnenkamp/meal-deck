package app.mealdeck.service;

import java.io.IOException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import app.mealdeck.dto.PackingSlipDraft;
import app.mealdeck.dto.PackingSlipResponse;

/** Extracts a reviewable shipment manifest from a packing-slip photo. */
@Service
public class PackingSlipExtractionService {
    private static final String PROMPT = """
            Read this Suvie packing slip. Extract the order number, printed ship date, and every
            row in the item table. Preserve each item code exactly. Quantity is the Qty column,
            not a number inferred from the description. Keep the description text factual and
            use null for unreadable values. Do not include totals, dry ice, or barcode digits as
            meal rows.
            """;

    private final ChatClient chatClient;
    private final PackingSlipMatcher matcher;
    private final String apiKey;
    private final String model;

    /**
     * @param chatClientBuilder auto-configured Spring AI client
     * @param matcher local reviewed-template matcher
     * @param apiKey configured provider credential
     * @param model multimodal extraction model
     */
    public PackingSlipExtractionService(
            ChatClient.Builder chatClientBuilder,
            PackingSlipMatcher matcher,
            @Value("${mealdeck.openai.api-key:}") String apiKey,
            @Value("${mealdeck.openai.model:gpt-5.6-sol}") String model) {
        this.chatClient = chatClientBuilder.build();
        this.matcher = matcher;
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * Extracts and classifies a packing slip without changing inventory.
     *
     * @param slip packing-slip image
     * @return reviewable shipment manifest
     */
    public PackingSlipResponse extract(MultipartFile slip) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Packing-slip extraction is not configured. Set OPENAI_API_KEY on the backend.");
        }
        try {
            Media slipImage = image(slip);
            PackingSlipDraft draft = chatClient.prompt()
                    .options(OpenAiChatOptions.builder().model(model).store(false))
                    .user(user -> user.text(PROMPT).media(slipImage))
                    .call()
                    .entity(PackingSlipDraft.class,
                            options -> options.useProviderStructuredOutput());
            if (draft == null) throw new IllegalStateException("Empty packing-slip extraction");
            return matcher.match(draft);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "The packing slip could not be read. Retake it flat in good light and try again.",
                    ex);
        }
    }

    private Media image(MultipartFile file) throws IOException {
        MimeType mimeType = file.getContentType() == null
                ? MimeTypeUtils.IMAGE_JPEG
                : MimeTypeUtils.parseMimeType(file.getContentType());
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
        return new Media(mimeType, resource);
    }
}
