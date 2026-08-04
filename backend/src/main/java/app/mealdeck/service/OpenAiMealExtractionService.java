package app.mealdeck.service;

import java.io.IOException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import app.mealdeck.dto.MealExtractionResponse;

@Service
/**
 * Uses Spring AI multimodal chat to extract structured meal-card data.
 */
public class OpenAiMealExtractionService implements MealExtractionService {
    static final String PROMPT = """
            Extract one prepared meal from these two images. The first is the meal-card front;
            the second is the cooking-guide back. Return only facts visible in the images.
            Nutrition must be per serving, never whole-package totals. Use null when unreadable.
            The front card uses two distinct title lines. Set name to only the prominent bold
            primary title, such as "Sweet Thai Chili Crab Cakes". Never append the smaller
            subtitle beginning with "with" to name. Set sides to the subtitle content after the
            leading "with", such as "Green Peas" or "White Rice and Broccoli". If no subtitle is visible, use null;
            do not invent or summarize one. Category should be a short useful grouping such as
            Chicken, Beef, Seafood, Pasta, or Vegetarian.
            Read the printed appliance cooking meal code independently from the bottom of the
            front card and from the cooking-guide back. Preserve every character, including
            leading zeroes, letters, punctuation, and capitalization. Set frontCookingMealCode
            and backCookingMealCode to their respective visible values. Set cookingMealCode only
            when both readable values match exactly or only one side is readable; otherwise use
            null so the user must resolve the conflict. Do not infer codes from meal names.
            Set frontBarcodePayload and backQrPayload to null; native barcode decoding supplies
            those fields separately.
            """;

    private final ChatClient chatClient;
    private final BarcodeDecoder barcodeDecoder;
    private final String apiKey;
    private final String model;

    /**
     * Creates the OpenAI-backed extractor.
     *
     * @param chatClientBuilder auto-configured Spring AI client builder
     * @param barcodeDecoder native card-identifier decoder
     * @param apiKey configured provider credential, used to detect disabled extraction
     * @param model OpenAI model used for multimodal extraction
     */
    public OpenAiMealExtractionService(
            ChatClient.Builder chatClientBuilder,
            BarcodeDecoder barcodeDecoder,
            @Value("${mealdeck.openai.api-key:}") String apiKey,
            @Value("${mealdeck.openai.model:gpt-5.6-sol}") String model) {
        this.chatClient = chatClientBuilder.build();
        this.barcodeDecoder = barcodeDecoder;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    /**
     * Sends both images in one non-stored request and maps native structured
     * output into a review DTO.
     *
     * @param front meal-card front photo
     * @param back cooking-guide back photo
     * @return extracted visible fields and per-serving nutrition
     */
    public MealExtractionResponse extract(MultipartFile front, MultipartFile back) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Photo extraction is not configured. Set OPENAI_API_KEY on the backend.");
        }
        try {
            Media frontImage = image(front);
            Media backImage = image(back);
            MealExtractionResponse response = chatClient.prompt()
                    .options(OpenAiChatOptions.builder()
                            .model(model)
                            .store(false))
                    .user(user -> user
                            .text(PROMPT)
                            .media(frontImage, backImage))
                    .call()
                    .entity(MealExtractionResponse.class,
                            options -> options.useProviderStructuredOutput());
            if (response == null) {
                throw new IllegalStateException("Empty extraction response");
            }
            return withMachineIdentifiers(
                    response,
                    barcodeDecoder.decodeFrontBarcode(front),
                    barcodeDecoder.decodeBackQr(back));
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The meal photos could not be read. Retake them in good light and try again.", ex);
        }
    }

    private MealExtractionResponse withMachineIdentifiers(
            MealExtractionResponse response,
            String frontBarcodePayload,
            String backQrPayload) {
        String[] title = separateTitle(response.name(), response.sides());
        return new MealExtractionResponse(
                title[0],
                title[1],
                response.category(),
                response.frontCookingMealCode(),
                response.backCookingMealCode(),
                response.cookingMealCode(),
                frontBarcodePayload,
                backQrPayload,
                response.caloriesPerServing(),
                response.carbsPerServing(),
                response.proteinPerServing(),
                response.fatPerServing(),
                response.sodiumMgPerServing());
    }

    /** Separates the provider's consistently printed "with ..." subtitle. */
    static String[] separateTitle(String name, String sides) {
        if (name == null) return new String[] { null, withoutLeadingWith(sides) };
        int subtitleStart = name.toLowerCase(java.util.Locale.ROOT).indexOf(" with ");
        if (subtitleStart < 1) return new String[] { name, withoutLeadingWith(sides) };
        return new String[] {
                name.substring(0, subtitleStart).trim(),
                name.substring(subtitleStart + " with ".length()).trim()
        };
    }

    private static String withoutLeadingWith(String sides) {
        if (sides == null) return null;
        return sides.replaceFirst("(?i)^\\s*with\\s+", "").trim();
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
