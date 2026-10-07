package org.commcare.formplayer.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method that takes a
 * {@link org.commcare.formplayer.beans.SessionRequestBean} argument (i.e. a request that addresses a
 * form session by id). {@link org.commcare.formplayer.aspects.SessionOwnershipAspect} verifies,
 * before the method runs, that the authenticated caller owns the referenced session.
 *
 * Every session-id-keyed route must carry this annotation; SessionOwnershipWiringTest enforces that
 * structurally so a new route cannot silently omit the check.
 */
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ValidateSessionOwner {
}
