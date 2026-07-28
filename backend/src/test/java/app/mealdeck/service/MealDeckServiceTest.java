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
import app.mealdeck.repository.MealHistoryRepository;
import app.mealdeck.repository.MealRepository;

@SpringBootTest
class MealDeckServiceTest {
    @Autowired MealDeckService service;
    @Autowired MealRepository meals;
    @Autowired MealHistoryRepository history;

    @BeforeEach
    void clean() { history.deleteAll(); meals.deleteAll(); }

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
    void mealStoresFrontAndCookingGuideImages() {
        var request = new MealRequest(
                "Teriyaki Salmon", null, null, 1, 650, 62, 40, 25, 930,
                "/uploads/front.heic", "/uploads/back.heic", "PHOTO");

        var saved = service.addMeal(request);

        assertThat(saved.imageUrl()).isEqualTo("/uploads/front.heic");
        assertThat(saved.cookingGuideImageUrl()).isEqualTo("/uploads/back.heic");
    }

    @Test
    void duplicateMealConsolidatesQuantityAndUpdatesBothImages() {
        service.addMeal(new MealRequest(
                "Teriyaki Salmon", null, null, 1, 650, 62, 40, 25, 930,
                "/uploads/old-front.heic", "/uploads/old-back.heic", "PHOTO"));

        var consolidated = service.addMeal(new MealRequest(
                " teriyaki  salmon ", null, null, 2, 650, 62, 40, 25, 930,
                "/uploads/new-front.heic", "/uploads/new-back.heic", "PHOTO"));

        assertThat(consolidated.quantity()).isEqualTo(3);
        assertThat(consolidated.imageUrl()).isEqualTo("/uploads/new-front.heic");
        assertThat(consolidated.cookingGuideImageUrl()).isEqualTo("/uploads/new-back.heic");
        assertThat(meals.count()).isEqualTo(1);
    }

    private MealRequest request(String name, int quantity) {
        return new MealRequest(name, null, null, quantity, 500, 40, 30, 20, 700, null, null, "TEST");
    }
}
