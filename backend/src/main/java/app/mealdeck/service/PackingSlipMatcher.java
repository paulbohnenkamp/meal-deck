package app.mealdeck.service;

import java.util.List;

import org.springframework.stereotype.Component;

import app.mealdeck.dto.PackingSlipDraft;
import app.mealdeck.dto.PackingSlipLineDraft;
import app.mealdeck.dto.PackingSlipLineResponse;
import app.mealdeck.dto.PackingSlipResponse;
import app.mealdeck.dto.MealTemplateResponse;
import app.mealdeck.entity.Meal;

/** Enriches OCR shipment rows with conservative local template matches. */
@Component
public class PackingSlipMatcher {
    private final MealTemplateService templates;

    /**
     * @param templates reviewed template lookup
     */
    public PackingSlipMatcher(MealTemplateService templates) {
        this.templates = templates;
    }

    /**
     * Classifies every visible shipment row without changing inventory.
     *
     * @param draft provider-structured packing-slip OCR
     * @return reviewable classified manifest
     */
    public PackingSlipResponse match(PackingSlipDraft draft) {
        List<PackingSlipLineResponse> lines = safeLines(draft).stream()
                .map(this::matchLine)
                .toList();
        return new PackingSlipResponse(draft.orderId(), draft.shippedAt(), lines);
    }

    private PackingSlipLineResponse matchLine(PackingSlipLineDraft line) {
        MealTemplateResponse template = templates.find(line.itemCode()).orElse(null);
        String status;
        if (template == null) {
            status = "UNKNOWN";
        } else if (Meal.normalize(template.name()).equals(Meal.normalize(line.description()))) {
            status = "KNOWN";
        } else {
            status = "CHANGED";
        }
        int quantity = line.quantity() == null || line.quantity() < 1 ? 1 : line.quantity();
        return new PackingSlipLineResponse(
                line.itemCode(), quantity, line.description(), status, template);
    }

    private List<PackingSlipLineDraft> safeLines(PackingSlipDraft draft) {
        return draft.lines() == null ? List.of() : draft.lines();
    }
}
