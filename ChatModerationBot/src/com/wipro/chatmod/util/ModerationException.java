package com.wipro.chatmod.util;

/**
 * ModerationException
 * --------------------
 * Custom checked exception thrown by {@code MessageModeratorService} whenever
 * a {@code ChatMessage} arrives with null, empty, or blank content.
 *
 * It is a checked exception (extends Exception, not RuntimeException) so that
 * every caller of moderate() is forced to explicitly handle the null/empty
 * message scenario, in line with the assignment requirement that such
 * messages must be caught, logged, and skipped without stopping the consumer
 * thread.
 */
public class ModerationException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Builds the exception with a descriptive message explaining why
     * the message could not be moderated.
     *
     * @param message human readable reason for the failure
     */
    public ModerationException(String message) {
        super(message);
    }

    /**
     * Custom string representation used when printing the exception
     * to the console, e.g.:
     *   ModerationException: Message content cannot be null or empty
     */
    @Override
    public String toString() {
        return "ModerationException: " + getMessage();
    }
}
