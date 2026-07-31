package app.mealdeck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import app.mealdeck.dto.MealRequest;
import app.mealdeck.dto.ShipmentConfirmationRequest;
import app.mealdeck.dto.ShipmentLineRequest;
import app.mealdeck.repository.MealHistoryRepository;
import app.mealdeck.repository.MealRepository;
import app.mealdeck.repository.MealTemplateRepository;
import app.mealdeck.repository.ShipmentRepository;

/** Verifies atomic, idempotent shipment inventory updates. */
@SpringBootTest
class ShipmentServiceTest {
    @Autowired ShipmentService shipments;
    @Autowired ShipmentRepository shipmentRepository;
    @Autowired MealTemplateRepository templates;
    @Autowired MealRepository meals;
    @Autowired MealHistoryRepository history;
    @Autowired MealDeckService mealDeck;
    @Autowired MealTemplateService templateService;

    @BeforeEach
    void clean() {
        history.deleteAll();
        shipmentRepository.deleteAll();
        meals.deleteAll();
        templates.deleteAll();
    }

    @Test
    void confirmsEveryResolvedLineAndRejectsDuplicateOrder() {
        var fish = reviewed("F99", "Crispy Baked Lemon Herb Fish");
        var tacos = reviewed("D3H", "Meatless Chorizo Tacos");
        var request = new ShipmentConfirmationRequest(
                "R2533747606",
                "7/8/2026",
                List.of(
                        new ShipmentLineRequest(fish.id(), "F99", 2),
                        new ShipmentLineRequest(tacos.id(), "D3H", 1)));

        var result = shipments.confirm(request);

        assertThat(result.totalBoxes()).isEqualTo(3);
        assertThat(meals.findAll()).extracting(meal -> meal.getQuantity())
                .containsExactlyInAnyOrder(2, 1);
        assertThat(shipmentRepository.count()).isEqualTo(1);

        assertThatThrownBy(() -> shipments.confirm(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already been confirmed");
        assertThat(meals.findAll()).extracting(meal -> meal.getQuantity())
                .containsExactlyInAnyOrder(2, 1);
    }

    @Test
    void invalidLineRollsBackTheEntireShipment() {
        var fish = reviewed("F99", "Crispy Baked Lemon Herb Fish");
        var request = new ShipmentConfirmationRequest(
                "R-ROLLBACK",
                null,
                List.of(
                        new ShipmentLineRequest(fish.id(), "F99", 2),
                        new ShipmentLineRequest(UUID.randomUUID(), "UNKNOWN", 1)));

        assertThatThrownBy(() -> shipments.confirm(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("active reviewed template");
        assertThat(meals.count()).isZero();
        assertThat(shipmentRepository.count()).isZero();
    }

    private app.mealdeck.dto.MealTemplateResponse reviewed(String code, String name) {
        mealDeck.addMeal(new MealRequest(
                name, null, null, code, null, null, 0,
                500, 40, 30, 20, 700, null, null, "PHOTO"));
        meals.deleteAll();
        return templateService.lookup(code);
    }
}
