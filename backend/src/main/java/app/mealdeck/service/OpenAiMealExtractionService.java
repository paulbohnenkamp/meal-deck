package app.mealdeck.service;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import app.mealdeck.dto.MealExtractionResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class OpenAiMealExtractionService implements MealExtractionService {
    private static final String PROMPT = """
            Extract one prepared meal from these two images. The first is the meal-card front;
            the second is the cooking-guide back. Return only facts visible in the images.
            Nutrition must be per serving, never whole-package totals. Use null when unreadable.
            Description should be a short factual meal overview. Category should be a short
            useful grouping such as Chicken, Beef, Seafood, Pasta, or Vegetarian.
            """;

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public OpenAiMealExtractionService(
            ObjectMapper objectMapper,
            @Value("${mealdeck.openai.api-key:}") String apiKey,
            @Value("${mealdeck.openai.model:gpt-5.6-sol}") String model) {
        this.client = RestClient.create("https://api.openai.com/v1");
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public MealExtractionResponse extract(MultipartFile front, MultipartFile back) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Photo extraction is not configured. Set OPENAI_API_KEY on the backend.");
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "store", false,
                    "input", List.of(Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of("type", "input_text", "text", PROMPT),
                                    image(front),
                                    image(back)))),
                    "text", Map.of("format", Map.of(
                            "type", "json_schema",
                            "name", "meal_extraction",
                            "strict", true,
                            "schema", schema())));

            JsonNode response = client.post()
                    .uri("/responses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String json = outputText(response);
            return objectMapper.readValue(json, MealExtractionResponse.class);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The meal photos could not be read. Retake them in good light and try again.", ex);
        }
    }

    private Map<String, Object> image(MultipartFile file) throws IOException {
        String type = file.getContentType() == null ? "image/jpeg" : file.getContentType();
        return Map.of(
                "type", "input_image",
                "detail", "high",
                "image_url", "data:" + type + ";base64," + Base64.getEncoder().encodeToString(file.getBytes()));
    }

    private String outputText(JsonNode response) {
        if (response == null) throw new IllegalStateException("Empty extraction response");
        for (JsonNode output : response.path("output")) {
            if (!"message".equals(output.path("type").asText())) continue;
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())) return content.path("text").asText();
            }
        }
        throw new IllegalStateException("Extraction response did not contain output text");
    }

    private Map<String, Object> schema() {
        Map<String, Object> nullableString = Map.of("type", List.of("string", "null"));
        Map<String, Object> nullableInteger = Map.of("type", List.of("integer", "null"));
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "name", nullableString,
                        "description", nullableString,
                        "category", nullableString,
                        "caloriesPerServing", nullableInteger,
                        "carbsPerServing", nullableInteger,
                        "proteinPerServing", nullableInteger,
                        "fatPerServing", nullableInteger,
                        "sodiumMgPerServing", nullableInteger),
                "required", List.of("name", "description", "category", "caloriesPerServing",
                        "carbsPerServing", "proteinPerServing", "fatPerServing", "sodiumMgPerServing"));
    }
}
