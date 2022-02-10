package org.commcare.formplayer.filters;

import org.springframework.core.Ordered;

public class FilterOrder {
    public final static int FIRST = Ordered.HIGHEST_PRECEDENCE;
    /**
     * This is set via the `spring.security.filter.order` property
     * but is kept here for reference.
     */
    public final static int SECURITY = 0;
    public final static int SENTRY = 1;
    public final static int LOGGING = 2;
}
