package org.commcare.formplayer.exceptions;

/**
 * This exception is for application config exceptions that return HTML instead of text to be rendered.
 */
public class FormattedApplicationConfigException extends ApplicationConfigException {
    public FormattedApplicationConfigException(String message) {
        super(message);
    }
}
