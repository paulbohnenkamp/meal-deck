package app.mealdeck.exception;

import java.util.UUID;

/** Indicates that a requested inventory meal does not exist. */
public class MealNotFoundException extends RuntimeException {
    /** @param id missing meal identifier */
    public MealNotFoundException(UUID id) {
        super("Meal not found: " + id);
    }
}
