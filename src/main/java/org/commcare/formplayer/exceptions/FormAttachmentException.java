package org.commcare.formplayer.exceptions;

/**
 * Exception raised on an error related to form attachments for eg. an invalid size attachment
 */
public class FormAttachmentException extends RuntimeException {

    public FormAttachmentException(String message) {
        super(message);
    }
}
