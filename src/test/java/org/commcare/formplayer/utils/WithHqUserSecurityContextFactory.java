package org.commcare.formplayer.utils;

import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * A WithSecurityContextFactory that works with {@link WithHqUser}.
 *
 * @see WithHqUser
 */
public final class WithHqUserSecurityContextFactory implements WithSecurityContextFactory<WithHqUser> {

    @Override
    public SecurityContext createSecurityContext(WithHqUser withUser) {
        return createSecurityContext(new HqUserDetails(withUser));
    }

    public static SecurityContext createSecurityContext(HqUserDetails details) {
        HqUserDetailsBean principal = details.toBean();
        Authentication authentication = new PreAuthenticatedAuthenticationToken(principal,
                "sessionId", principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }

    public static void setSecurityContext(HqUserDetails details) {
        SecurityContext context = createSecurityContext(details);
        SecurityContextHolder.setContext(context);
    }
}
