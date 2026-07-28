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
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class MealDeckController {
    private final MealDeckService service;

    public MealDeckController(MealDeckService service) { this.service = service; }

    @GetMapping("/meals")
    public List<MealResponse> meals() { return service.listMeals(); }

    @PostMapping("/meals")
    @ResponseStatus(HttpStatus.CREATED)
    public MealResponse add(@Valid @RequestBody MealRequest request) { return service.addMeal(request); }

    @PutMapping("/meals/{id}")
    public MealResponse update(@PathVariable UUID id, @Valid @RequestBody MealRequest request) {
        return service.updateMeal(id, request);
    }

    @DeleteMapping("/meals/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.deleteMeal(id); }

    @PostMapping("/meals/{id}/consume")
    public PickResponse consume(@PathVariable UUID id, @RequestParam(defaultValue = "7") int avoidDays) {
        return service.consume(id, avoidDays);
    }

    @PostMapping("/picks/random")
    public PickResponse random(@RequestParam(defaultValue = "7") int avoidDays,
                               @RequestParam(defaultValue = "false") boolean allowRecent) {
        return service.pickRandom(avoidDays, allowRecent);
    }

    @PostMapping("/picks/preview")
    public MealResponse preview(@RequestParam(defaultValue = "7") int avoidDays,
                                @RequestParam(defaultValue = "false") boolean allowRecent,
                                @RequestParam(required = false) UUID excludeMealId) {
        return service.previewRandom(avoidDays, allowRecent, excludeMealId);
    }

    @GetMapping("/history")
    public List<HistoryResponse> history() { return service.listHistory(); }

    @PostMapping("/history/{id}/undo")
    public MealResponse undo(@PathVariable UUID id) { return service.undoHistory(id); }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@RequestParam(defaultValue = "7") int avoidDays) {
        return service.dashboard(avoidDays);
    }
}
