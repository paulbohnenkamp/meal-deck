package app.mealdeck.exception;

import java.util.UUID;

/** Indicates that a requested dinner-history entry does not exist. */
public class HistoryNotFoundException extends RuntimeException {
    /** @param id missing history identifier */
    public HistoryNotFoundException(UUID id) {
        super("History entry not found: " + id);
    }
}
