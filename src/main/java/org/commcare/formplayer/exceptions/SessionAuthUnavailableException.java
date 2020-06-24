package org.commcare.formplayer.exceptions;

/**
 * Thrown when FormPlayer identifies that the user's authentication is
 * unavailable and they will need a new session to continue
 */
public class SessionAuthUnavailableException extends RuntimeException {
}
