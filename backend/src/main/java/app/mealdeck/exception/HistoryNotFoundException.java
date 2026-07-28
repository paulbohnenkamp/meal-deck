package app.mealdeck.exception;

import java.util.UUID;

public class HistoryNotFoundException extends RuntimeException {
    public HistoryNotFoundException(UUID id) {
        super("History entry not found: " + id);
    }
}
