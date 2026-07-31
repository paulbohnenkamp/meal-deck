package app.mealdeck.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import app.mealdeck.dto.MealResponse;
import app.mealdeck.dto.ShipmentConfirmationRequest;
import app.mealdeck.dto.ShipmentConfirmationResponse;
import app.mealdeck.dto.ShipmentLineRequest;
import app.mealdeck.entity.Meal;
import app.mealdeck.entity.MealTemplate;
import app.mealdeck.entity.Shipment;
import app.mealdeck.mapper.MealMapper;
import app.mealdeck.repository.MealRepository;
import app.mealdeck.repository.MealTemplateRepository;
import app.mealdeck.repository.ShipmentRepository;

/** Confirms fully reviewed shipments as one idempotent inventory transaction. */
@Service
public class ShipmentService {
    private final ShipmentRepository shipments;
    private final MealTemplateRepository templates;
    private final MealRepository meals;
    private final MealMapper mealMapper;

    /**
     * @param shipments confirmed shipment records
     * @param templates reviewed template revisions
     * @param meals consolidated inventory
     * @param mealMapper inventory response mapper
     */
    public ShipmentService(
            ShipmentRepository shipments,
            MealTemplateRepository templates,
            MealRepository meals,
            MealMapper mealMapper) {
        this.shipments = shipments;
        this.templates = templates;
        this.meals = meals;
        this.mealMapper = mealMapper;
    }

    /**
     * Prevalidates every line, then adds every box and the idempotency record
     * in one database transaction.
     *
     * @param request fully reviewed shipment
     * @return consolidated inventory results
     */
    @Transactional
    public ShipmentConfirmationResponse confirm(ShipmentConfirmationRequest request) {
        String orderId = request.orderId().trim();
        if (shipments.existsByExternalOrderId(orderId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "That shipment has already been confirmed.");
        }

        List<ResolvedLine> resolved = request.lines().stream()
                .map(this::resolve)
                .toList();
        int totalBoxes = resolved.stream().mapToInt(ResolvedLine::quantity).sum();
        Map<String, ResolvedLine> consolidated = new LinkedHashMap<>();
        for (ResolvedLine line : resolved) {
            String key = Meal.normalize(line.template().getName());
            consolidated.merge(
                    key,
                    line,
                    (left, right) -> new ResolvedLine(left.template(), left.quantity() + right.quantity()));
        }

        List<MealResponse> updated = new ArrayList<>();
        for (ResolvedLine line : consolidated.values()) {
            updated.add(addInventory(line.template(), line.quantity()));
        }

        Shipment shipment = new Shipment();
        shipment.setExternalOrderId(orderId);
        shipment.setShippedAt(clean(request.shippedAt()));
        shipment.setTotalBoxes(totalBoxes);
        Shipment saved = shipments.saveAndFlush(shipment);
        return new ShipmentConfirmationResponse(
                orderId, totalBoxes, updated, saved.getConfirmedAt());
    }

    private ResolvedLine resolve(ShipmentLineRequest line) {
        if (line.quantity() < 1) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "Every shipment quantity must be at least one box.");
        }
        MealTemplate template = templates.findById(line.templateId())
                .filter(MealTemplate::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        "Every shipment row must reference an active reviewed template."));
        String itemCode = clean(line.itemCode());
        if (itemCode != null && !matchesIdentifier(template, itemCode)) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "A shipment item code no longer matches its reviewed template.");
        }
        return new ResolvedLine(template, line.quantity());
    }

    private boolean matchesIdentifier(MealTemplate template, String value) {
        return value.equals(template.getCookingMealCode())
                || value.equals(template.getFrontBarcodePayload())
                || value.equals(template.getBackQrPayload());
    }

    private MealResponse addInventory(MealTemplate template, int quantity) {
        String normalized = Meal.normalize(template.getName());
        Meal meal = meals.findByNormalizedName(normalized).orElseGet(Meal::new);
        boolean existing = meal.getId() != null;
        meal.setName(template.getName());
        meal.setDescription(template.getDescription());
        meal.setCategory(template.getCategory());
        meal.setCookingMealCode(template.getCookingMealCode());
        meal.setFrontBarcodePayload(template.getFrontBarcodePayload());
        meal.setBackQrPayload(template.getBackQrPayload());
        meal.setQuantity((existing ? meal.getQuantity() : 0) + quantity);
        meal.setServings(2);
        meal.setCaloriesPerServing(template.getCaloriesPerServing());
        meal.setCarbsPerServing(template.getCarbsPerServing());
        meal.setProteinPerServing(template.getProteinPerServing());
        meal.setFatPerServing(template.getFatPerServing());
        meal.setSodiumMgPerServing(template.getSodiumMgPerServing());
        meal.setImageUrl(template.getImageUrl());
        meal.setCookingGuideImageUrl(template.getCookingGuideImageUrl());
        meal.setSource("SHIPMENT");
        return mealMapper.toResponse(meals.save(meal));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ResolvedLine(MealTemplate template, int quantity) {}
}
