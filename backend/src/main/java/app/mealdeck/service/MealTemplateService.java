package app.mealdeck.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import app.mealdeck.dto.MealRequest;
import app.mealdeck.dto.MealTemplateResponse;
import app.mealdeck.entity.MealTemplate;
import app.mealdeck.repository.MealTemplateRepository;

/** Creates immutable template revisions and resolves reviewed provider aliases. */
@Service
public class MealTemplateService {
    private static final String PROVIDER = "SUVIE";
    private final MealTemplateRepository templates;

    /**
     * @param templates meal-template persistence
     */
    public MealTemplateService(MealTemplateRepository templates) {
        this.templates = templates;
    }

    /**
     * Returns the active reviewed template matching any provider identifier.
     *
     * @param identifier cooking code, barcode payload, or QR payload
     * @return matching active template revision
     */
    @Transactional(readOnly = true)
    public MealTemplateResponse lookup(String identifier) {
        return find(identifier).orElseThrow(this::notFound);
    }

    /**
     * Finds a unique active template without converting absence to an HTTP error.
     *
     * @param identifier cooking code, barcode payload, or QR payload
     * @return matching template, or empty when it has not been reviewed
     */
    @Transactional(readOnly = true)
    public Optional<MealTemplateResponse> find(String identifier) {
        String value = clean(identifier);
        if (value == null) return Optional.empty();
        List<MealTemplate> matches = activeMatches(value);
        if (matches.isEmpty()) return Optional.empty();
        if (matches.size() > 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "That identifier matches more than one reviewed meal template.");
        }
        return Optional.of(response(matches.getFirst()));
    }

    /**
     * Saves a new immutable revision when reviewed definition fields changed.
     *
     * @param request confirmed inventory meal values
     */
    @Transactional
    public void remember(MealRequest request) {
        if (!hasIdentifier(request)) return;
        MealTemplate current = findCurrent(request);
        if (current != null && sameDefinition(current, request)) return;
        if (current != null) current.setActive(false);

        MealTemplate revision = new MealTemplate();
        revision.setProvider(PROVIDER);
        revision.setRevision(current == null ? 1 : current.getRevision() + 1);
        revision.setActive(true);
        copy(request, revision);
        templates.save(revision);
    }

    private MealTemplate findCurrent(MealRequest request) {
        return identifiers(request).stream()
                .flatMap(value -> activeMatches(value).stream())
                .distinct()
                .findFirst()
                .orElse(null);
    }

    private List<MealTemplate> activeMatches(String value) {
        return java.util.stream.Stream.of(
                        templates.findByProviderAndActiveTrueAndCookingMealCode(PROVIDER, value),
                        templates.findByProviderAndActiveTrueAndFrontBarcodePayload(PROVIDER, value),
                        templates.findByProviderAndActiveTrueAndBackQrPayload(PROVIDER, value))
                .flatMap(List::stream)
                .distinct()
                .toList();
    }

    private List<String> identifiers(MealRequest request) {
        return java.util.stream.Stream.of(
                        clean(request.cookingMealCode()),
                        clean(request.frontBarcodePayload()),
                        clean(request.backQrPayload()))
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean hasIdentifier(MealRequest request) {
        return !identifiers(request).isEmpty();
    }

    private void copy(MealRequest request, MealTemplate template) {
        template.setCookingMealCode(clean(request.cookingMealCode()));
        template.setFrontBarcodePayload(clean(request.frontBarcodePayload()));
        template.setBackQrPayload(clean(request.backQrPayload()));
        template.setName(request.name().trim());
        template.setSides(request.sides());
        template.setCategory(request.category());
        template.setCaloriesPerServing(request.caloriesPerServing());
        template.setCarbsPerServing(request.carbsPerServing());
        template.setProteinPerServing(request.proteinPerServing());
        template.setFatPerServing(request.fatPerServing());
        template.setSodiumMgPerServing(request.sodiumMgPerServing());
        template.setImageUrl(request.imageUrl());
        template.setCookingGuideImageUrl(request.cookingGuideImageUrl());
    }

    private boolean sameDefinition(MealTemplate value, MealRequest request) {
        return Objects.equals(value.getCookingMealCode(), clean(request.cookingMealCode()))
                && Objects.equals(value.getFrontBarcodePayload(), clean(request.frontBarcodePayload()))
                && Objects.equals(value.getBackQrPayload(), clean(request.backQrPayload()))
                && Objects.equals(value.getName(), request.name().trim())
                && Objects.equals(value.getSides(), request.sides())
                && Objects.equals(value.getCategory(), request.category())
                && Objects.equals(value.getCaloriesPerServing(), request.caloriesPerServing())
                && Objects.equals(value.getCarbsPerServing(), request.carbsPerServing())
                && Objects.equals(value.getProteinPerServing(), request.proteinPerServing())
                && Objects.equals(value.getFatPerServing(), request.fatPerServing())
                && Objects.equals(value.getSodiumMgPerServing(), request.sodiumMgPerServing())
                && Objects.equals(value.getImageUrl(), request.imageUrl())
                && Objects.equals(value.getCookingGuideImageUrl(), request.cookingGuideImageUrl());
    }

    private MealTemplateResponse response(MealTemplate value) {
        return new MealTemplateResponse(
                value.getId(), value.getProvider(), value.getRevision(),
                value.getCookingMealCode(), value.getFrontBarcodePayload(), value.getBackQrPayload(),
                value.getName(), value.getSides(), value.getCategory(),
                value.getCaloriesPerServing(), value.getCarbsPerServing(),
                value.getProteinPerServing(), value.getFatPerServing(),
                value.getSodiumMgPerServing(), value.getImageUrl(),
                value.getCookingGuideImageUrl(), value.getVerifiedAt());
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, "No reviewed meal template matches that identifier.");
    }
}
