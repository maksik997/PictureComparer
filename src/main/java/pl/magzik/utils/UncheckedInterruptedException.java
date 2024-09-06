package pl.magzik.utils;

import java.io.UncheckedIOException;
import java.util.Objects;


/**
 * Based on {@link UncheckedIOException}.
 * <p>
 * Wraps an {@link InterruptedException} with an unchecked exception.
 */
public class UncheckedInterruptedException extends RuntimeException {

    /**
     * Constructs an instance of this class.
     * @param message
     *          the detail message, can be null
     * @param cause
     *          the {@code InterruptedException}
     *
     * @throws NullPointerException
     *          if the cause is {@code null}
     */
    public UncheckedInterruptedException(String message, InterruptedException cause) {
        super(message, Objects.requireNonNull(cause));
    }

    /**
     * Constructs an instance of this class.
     * @param cause
     *          the {@code InterruptedException}
     *
     * @throws NullPointerException
     *          if the cause is {@code null}
     */
    public UncheckedInterruptedException(InterruptedException cause) {
        super(Objects.requireNonNull(cause));
    }

    /**
     * Returns the cause of this exception.
     * @return the {@code InterruptedException} which is the cause of this exception.
     */
    @Override
    public synchronized InterruptedException getCause() {
        return (InterruptedException) super.getCause();
    }
}
