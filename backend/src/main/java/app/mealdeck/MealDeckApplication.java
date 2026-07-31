package app.mealdeck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
/**
 * Bootstraps the MealDeck Spring application.
 */
public class MealDeckApplication {
    /**
     * Starts the embedded web server and application context.
     *
     * @param args command-line arguments passed to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(MealDeckApplication.class, args);
    }
}
