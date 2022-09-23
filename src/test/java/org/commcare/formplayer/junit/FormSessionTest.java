package org.commcare.formplayer.junit;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Convenience annotation for tests that interact with form sessions.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith({
        InitializeStaticsExtension.class,
        FormDefSessionServiceExtension.class,
        FormSessionServiceExtension.class,
})
public @interface FormSessionTest {
}
