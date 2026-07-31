package app.mealdeck.exception;

/** Indicates that an update would collide with another normalized meal name. */
public class DuplicateMealNameException extends RuntimeException {
    /**
     * Creates the domain exception around the persistence failure.
     *
     * @param cause underlying uniqueness failure
     */
    public DuplicateMealNameException(Throwable cause) {
        super("Another meal already uses that name", cause);
    }
}
