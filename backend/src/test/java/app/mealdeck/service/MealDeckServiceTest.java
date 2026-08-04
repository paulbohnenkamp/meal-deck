package app.mealdeck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import app.mealdeck.dto.MealRequest;
import app.mealdeck.entity.Meal;
import app.mealdeck.entity.MealHistory;
import app.mealdeck.exception.InventoryConflictException;
import app.mealdeck.exception.DuplicateMealNameException;
import app.mealdeck.repository.MealHistoryRepository;
import app.mealdeck.repository.MealRepository;
import app.mealdeck.repository.MealTemplateRepository;

/** Exercises the transactional inventory and history domain rules. */
@SpringBootTest
class MealDeckServiceTest {
    @Autowired MealDeckService service;
    @Autowired MealRepository meals;
    @Autowired MealHistoryRepository history;
    @Autowired MealTemplateRepository templates;
    @Autowired MealTemplateService templateService;

    @BeforeEach
    void clean() { history.deleteAll(); meals.deleteAll(); templates.deleteAll(); }

    @Test
    void randomPreviewSkipsRecentlyEatenAndOnlyConfirmationMutatesInventory() {
        var recent = service.addMeal(request("Meal A", 2));
        var eligible = service.addMeal(request("Meal B", 1));

        MealHistory eaten = new MealHistory();
        eaten.setMealId(recent.id());
        eaten.setMealName(recent.name());
        eaten.setNormalizedMealName(Meal.normalize(recent.name()));
        eaten.setServingsSnapshot(2);
        eaten.setConsumedAt(Instant.now());
        history.save(eaten);

        var preview = service.previewRandom(7, false, null);
        assertThat(preview.id()).isEqualTo(eligible.id());
        assertThat(meals.findById(eligible.id()).orElseThrow().getQuantity()).isEqualTo(1);
        assertThat(service.listHistory()).hasSize(1);

        service.consume(preview.id(), 7);
        assertThat(meals.findById(eligible.id()).orElseThrow().getQuantity()).isZero();
        assertThat(service.listHistory()).hasSize(2);
    }

    @Test
    void strictPickExplainsWhenEveryMealIsRecent() {
        var only = service.addMeal(request("Only Meal", 1));
        MealHistory eaten = new MealHistory();
        eaten.setMealId(only.id());
        eaten.setMealName(only.name());
        eaten.setNormalizedMealName(Meal.normalize(only.name()));
        eaten.setServingsSnapshot(2);
        eaten.setConsumedAt(Instant.now());
        history.save(eaten);

        assertThatThrownBy(() -> service.previewRandom(7, false, null))
                .isInstanceOf(InventoryConflictException.class)
                .hasMessageContaining("relaxed draw");
        assertThat(service.previewRandom(7, true, null).name()).isEqualTo("Only Meal");
        assertThat(meals.findById(only.id()).orElseThrow().getQuantity()).isEqualTo(1);
        assertThat(service.listHistory()).hasSize(1);
    }

    @Test
    void dashboardDoesNotCountAnOutOfStockMealType() {
        var meal = service.addMeal(request("Last box", 1));
        assertThat(service.dashboard(7).mealTypes()).isEqualTo(1);

        service.consume(meal.id(), 7);

        var dashboard = service.dashboard(7);
        assertThat(dashboard.totalBoxes()).isZero();
        assertThat(dashboard.mealTypes()).isZero();
        assertThat(dashboard.eligibleMealTypes()).isZero();
        assertThat(service.listHistory()).hasSize(1);
    }

    @Test
    void mealStoresFrontAndCookingGuideImages() {
        var request = new MealRequest(
                "Teriyaki Salmon", null, null, " 012-A ", " 310012345678 ",
                " https://suvie.com/m/012-A ", 1, 650, 62, 40, 25, 930,
                "/uploads/front.heic", "/uploads/back.heic", "PHOTO");

        var saved = service.addMeal(request);

        assertThat(saved.imageUrl()).isEqualTo("/uploads/front.heic");
        assertThat(saved.cookingGuideImageUrl()).isEqualTo("/uploads/back.heic");
        assertThat(saved.cookingMealCode()).isEqualTo("012-A");
        assertThat(saved.frontBarcodePayload()).isEqualTo("310012345678");
        assertThat(saved.backQrPayload()).isEqualTo("https://suvie.com/m/012-A");
    }

    @Test
    void duplicateMealConsolidatesQuantityAndUpdatesBothImages() {
        service.addMeal(new MealRequest(
                "Teriyaki Salmon", null, null, "OLD-1", null, null, 1, 650, 62, 40, 25, 930,
                "/uploads/old-front.heic", "/uploads/old-back.heic", "PHOTO"));

        var consolidated = service.addMeal(new MealRequest(
                " teriyaki  salmon ", null, null, "NEW-2", null, null, 2, 650, 62, 40, 25, 930,
                "/uploads/new-front.heic", "/uploads/new-back.heic", "PHOTO"));

        assertThat(consolidated.quantity()).isEqualTo(3);
        assertThat(consolidated.imageUrl()).isEqualTo("/uploads/new-front.heic");
        assertThat(consolidated.cookingGuideImageUrl()).isEqualTo("/uploads/new-back.heic");
        assertThat(consolidated.cookingMealCode()).isEqualTo("NEW-2");
        assertThat(meals.count()).isEqualTo(1);
    }

    @Test
    void reviewedIdentifiersCreateReusableImmutableRevisions() {
        var first = new MealRequest(
                "Teriyaki Salmon", "Salmon with rice", "Seafood", "012-A",
                "310012345678", "https://suvie.com/m/012-A", 1,
                510, 42, 31, 18, 850, "/front.jpg", "/back.jpg", "PHOTO");
        service.addMeal(first);

        var saved = templateService.lookup("310012345678");
        assertThat(saved.name()).isEqualTo("Teriyaki Salmon");
        assertThat(saved.revision()).isEqualTo(1);

        service.addMeal(first);
        assertThat(templates.count()).isEqualTo(1);

        var changed = new MealRequest(
                "Teriyaki Salmon", "Salmon with brown rice", "Seafood", "012-A",
                "310012345678", "https://suvie.com/m/012-A", 1,
                525, 44, 31, 18, 850, "/front-v2.jpg", "/back-v2.jpg", "PHOTO");
        service.addMeal(changed);

        var latest = templateService.lookup("https://suvie.com/m/012-A");
        assertThat(latest.revision()).isEqualTo(2);
        assertThat(latest.sides()).isEqualTo("Salmon with brown rice");
        assertThat(latest.caloriesPerServing()).isEqualTo(525);
        assertThat(templates.count()).isEqualTo(2);
        assertThat(templates.findAll()).filteredOn(template -> template.isActive()).hasSize(1);
    }

    @Test
    void updateReplacesEveryEditableFieldAndPreservesTwoServings() {
        var saved = service.addMeal(new MealRequest(
                "Old meal", null, null, "007-A", null, null, 1,
                500, 40, 30, 20, 700, null, null, "TEST"));
        var replacement = new MealRequest(
                "New meal", "Updated sides", "Vegetarian", "007-A",
                "001234567890", "https://example.test/meal/007-A", 3,
                420, 38, 27, 16, 760, "/new-front.heic", "/new-back.heic", "EDITED");

        var updated = service.updateMeal(saved.id(), replacement);

        assertThat(updated.name()).isEqualTo("New meal");
        assertThat(updated.sides()).isEqualTo("Updated sides");
        assertThat(updated.category()).isEqualTo("Vegetarian");
        assertThat(updated.cookingMealCode()).isEqualTo("007-A");
        assertThat(updated.frontBarcodePayload()).isEqualTo("001234567890");
        assertThat(updated.backQrPayload()).isEqualTo("https://example.test/meal/007-A");
        assertThat(updated.quantity()).isEqualTo(3);
        assertThat(updated.servings()).isEqualTo(2);
        assertThat(updated.caloriesPerServing()).isEqualTo(420);
        assertThat(updated.carbsPerServing()).isEqualTo(38);
        assertThat(updated.proteinPerServing()).isEqualTo(27);
        assertThat(updated.fatPerServing()).isEqualTo(16);
        assertThat(updated.sodiumMgPerServing()).isEqualTo(760);
        assertThat(updated.imageUrl()).isEqualTo("/new-front.heic");
        assertThat(updated.cookingGuideImageUrl()).isEqualTo("/new-back.heic");
    }

    @Test
    void exactCookingCodeMatchesExistingInventoryBeforeEditedName() {
        service.addMeal(new MealRequest(
                "Sweet Thai Chili Crab Cakes", "with Green Peas", "Seafood", "F99",
                null, null, 1, 530, 71, 13, 17, 1100, null, null, "PHOTO"));

        var consolidated = service.addMeal(new MealRequest(
                "Edited display name", "with Green Peas", "Seafood", "F99",
                null, null, 2, 530, 71, 13, 17, 1100, null, null, "PHOTO"));

        assertThat(consolidated.quantity()).isEqualTo(3);
        assertThat(meals.count()).isEqualTo(1);
        assertThat(consolidated.cookingMealCode()).isEqualTo("F99");
    }

    @Test
    void updateRejectsChangingAConfirmedCookingCode() {
        var saved = service.addMeal(new MealRequest(
                "Meal", null, null, "007-A", null, null, 1,
                500, 40, 30, 20, 700, null, null, "TEST"));

        assertThatThrownBy(() -> service.updateMeal(saved.id(), new MealRequest(
                "Meal", null, null, "008-B", null, null, 1,
                500, 40, 30, 20, 700, null, null, "TEST")))
                .isInstanceOf(InventoryConflictException.class)
                .hasMessageContaining("cannot be changed");
    }

    @Test
    void updateRejectsAnotherMealsNormalizedName() {
        var first = service.addMeal(request("Meal One", 1));
        service.addMeal(request("Meal Two", 1));

        assertThatThrownBy(() -> service.updateMeal(first.id(), request(" meal--two ", 1)))
                .isInstanceOf(DuplicateMealNameException.class);
        assertThat(meals.count()).isEqualTo(2);
    }

    private MealRequest request(String name, int quantity) {
        return new MealRequest(name, null, null, null, null, null, quantity,
                500, 40, 30, 20, 700, null, null, "TEST");
    }
}
