package exceptions;

import org.kxml2.io.KXmlParser;

/**
 * InvalidStructureRuntimeExceptions wraps InvalidStructureException for methods
 * where passing a typed exception is not possible
 *
 * @author wpride
 */
public class InvalidStructureRuntimeException extends RuntimeException {
    public InvalidStructureRuntimeException(String message) {
        super(message);
    }
}
