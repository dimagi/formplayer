package org.commcare.formplayer.application;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Controller advice to replace the default error handling with simple responses
 * instead of rendering error pages.
 */
@ControllerAdvice()
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
}
