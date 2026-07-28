package app.mealdeck.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import app.mealdeck.entity.Meal;
import app.mealdeck.repository.MealRepository;

@Configuration
public class SeedData {
    @Bean
    CommandLineRunner seedMeals(MealRepository meals) {
        return args -> {
            if (meals.count() > 0) return;
            meals.save(meal("Chicken Alfredo", "Pasta with roasted chicken", "Pasta", 2, 610, 54, 35, 28, 940));
            meals.save(meal("Beef Bulgogi", "Korean-style beef with rice", "Beef", 1, 540, 62, 29, 19, 1080));
            meals.save(meal("Tuscan Chicken", "Chicken, vegetables and creamy sauce", "Chicken", 2, 430, 21, 38, 22, 790));
        };
    }

    private Meal meal(String name, String description, String category, int quantity, int calories,
                      int carbs, int protein, int fat, int sodium) {
        Meal meal = new Meal();
        meal.setName(name);
        meal.setDescription(description);
        meal.setCategory(category);
        meal.setQuantity(quantity);
        meal.setServings(2);
        meal.setCaloriesPerServing(calories);
        meal.setCarbsPerServing(carbs);
        meal.setProteinPerServing(protein);
        meal.setFatPerServing(fat);
        meal.setSodiumMgPerServing(sodium);
        meal.setSource("SAMPLE");
        return meal;
    }
}
