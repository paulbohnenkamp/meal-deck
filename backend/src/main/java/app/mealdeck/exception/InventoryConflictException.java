package app.mealdeck.exception;

/** Indicates that current inventory or history prevents the requested action. */
public class InventoryConflictException extends RuntimeException {
    /** @param message user-facing explanation of the conflict */
    public InventoryConflictException(String message) {
        super(message);
    }
}
