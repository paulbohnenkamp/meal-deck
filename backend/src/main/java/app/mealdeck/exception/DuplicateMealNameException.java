package app.mealdeck.exception;

public class DuplicateMealNameException extends RuntimeException {
    public DuplicateMealNameException(Throwable cause) {
        super("Another meal already uses that name", cause);
    }
}
