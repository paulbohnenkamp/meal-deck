package app.mealdeck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import app.mealdeck.api.MealDtos.MealRequest;
import app.mealdeck.history.MealHistory;
import app.mealdeck.history.MealHistoryRepository;
import app.mealdeck.meal.Meal;
import app.mealdeck.meal.MealRepository;

@SpringBootTest
class MealDeckServiceTest {
    @Autowired MealDeckService service;
    @Autowired MealRepository meals;
    @Autowired MealHistoryRepository history;

    @BeforeEach
    void clean() { history.deleteAll(); meals.deleteAll(); }

    @Test
    void randomPickSkipsRecentlyEatenAndDecrementsInventory() {
        var recent = service.addMeal(request("Meal A", 2));
        var eligible = service.addMeal(request("Meal B", 1));

        MealHistory eaten = new MealHistory();
        eaten.setMealId(recent.id());
        eaten.setMealName(recent.name());
        eaten.setNormalizedMealName(Meal.normalize(recent.name()));
        eaten.setServingsSnapshot(2);
        eaten.setConsumedAt(Instant.now());
        history.save(eaten);

        var pick = service.pickRandom(7, false);
        assertThat(pick.meal().id()).isEqualTo(eligible.id());
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

        assertThatThrownBy(() -> service.pickRandom(7, false))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("relaxed draw");
        assertThat(service.pickRandom(7, true).meal().name()).isEqualTo("Only Meal");
    }

    private MealRequest request(String name, int quantity) {
        return new MealRequest(name, null, null, quantity, 500, 40, 30, 20, 700, null, "TEST");
    }
}
