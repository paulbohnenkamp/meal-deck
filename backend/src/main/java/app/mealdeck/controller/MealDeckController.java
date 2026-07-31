package app.mealdeck.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import app.mealdeck.dto.DashboardResponse;
import app.mealdeck.dto.HistoryResponse;
import app.mealdeck.dto.MealRequest;
import app.mealdeck.dto.MealResponse;
import app.mealdeck.dto.PickResponse;
import app.mealdeck.service.MealDeckService;
import app.mealdeck.dto.MealTemplateResponse;
import app.mealdeck.service.MealTemplateService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
/**
 * Defines the inventory, dinner-selection, history, and dashboard REST API.
 */
public class MealDeckController {
    private final MealDeckService service;
    private final MealTemplateService templates;

    /**
     * Creates the API controller.
     *
     * @param service transactional MealDeck domain service
     * @param templates reusable meal-template service
     */
    public MealDeckController(MealDeckService service, MealTemplateService templates) {
        this.service = service;
        this.templates = templates;
    }

    /** @return all inventory meals sorted by name */
    @GetMapping("/meals")
    public List<MealResponse> meals() { return service.listMeals(); }

    /**
     * Adds a meal box or consolidates it with an existing normalized name.
     *
     * @param request validated meal values
     * @return current consolidated meal
     */
    @PostMapping("/meals")
    @ResponseStatus(HttpStatus.CREATED)
    public MealResponse add(@Valid @RequestBody MealRequest request) { return service.addMeal(request); }

    /**
     * Finds a reusable reviewed meal by cooking code, barcode, or QR payload.
     *
     * @param identifier exact provider identifier
     * @return active template revision
     */
    @GetMapping("/meal-templates/lookup")
    public MealTemplateResponse template(@RequestParam String identifier) {
        return templates.lookup(identifier);
    }

    /**
     * Replaces editable values for an inventory meal.
     *
     * @param id meal identifier
     * @param request validated replacement values
     * @return updated meal
     */
    @PutMapping("/meals/{id}")
    public MealResponse update(@PathVariable UUID id, @Valid @RequestBody MealRequest request) {
        return service.updateMeal(id, request);
    }

    /** @param id identifier of the meal to remove */
    @DeleteMapping("/meals/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.deleteMeal(id); }

    /**
     * Consumes one box and records dinner history atomically.
     *
     * @param id meal identifier
     * @param avoidDays recent-history window reported with the result
     * @return consumed meal and new history entry
     */
    @PostMapping("/meals/{id}/consume")
    public PickResponse consume(@PathVariable UUID id, @RequestParam(defaultValue = "7") int avoidDays) {
        return service.consume(id, avoidDays);
    }

    /**
     * Chooses and immediately consumes an eligible random meal.
     *
     * @param avoidDays number of recent days excluded by a strict draw
     * @param allowRecent whether recently eaten names may be selected
     * @return consumed meal and new history entry
     */
    @PostMapping("/picks/random")
    public PickResponse random(@RequestParam(defaultValue = "7") int avoidDays,
                               @RequestParam(defaultValue = "false") boolean allowRecent) {
        return service.pickRandom(avoidDays, allowRecent);
    }

    /**
     * Chooses an eligible meal without mutating inventory.
     *
     * @param avoidDays number of recent days excluded by a strict draw
     * @param allowRecent whether recently eaten names may be selected
     * @param excludeMealId optional prior preview to avoid when alternatives exist
     * @return randomly selected meal
     */
    @PostMapping("/picks/preview")
    public MealResponse preview(@RequestParam(defaultValue = "7") int avoidDays,
                                @RequestParam(defaultValue = "false") boolean allowRecent,
                                @RequestParam(required = false) UUID excludeMealId) {
        return service.previewRandom(avoidDays, allowRecent, excludeMealId);
    }

    /** @return dinner history in reverse chronological order */
    @GetMapping("/history")
    public List<HistoryResponse> history() { return service.listHistory(); }

    /**
     * Removes a history entry and restores one corresponding box.
     *
     * @param id history identifier
     * @return restored inventory meal
     */
    @PostMapping("/history/{id}/undo")
    public MealResponse undo(@PathVariable UUID id) { return service.undoHistory(id); }

    /**
     * Summarizes inventory and strict-draw eligibility.
     *
     * @param avoidDays recent-history exclusion window
     * @return dashboard counts
     */
    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@RequestParam(defaultValue = "7") int avoidDays) {
        return service.dashboard(avoidDays);
    }
}
