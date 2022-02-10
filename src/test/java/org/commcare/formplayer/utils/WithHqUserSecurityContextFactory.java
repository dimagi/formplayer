package org.commcare.formplayer.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A WithSecurityContextFactory that works with {@link WithHqUser}.
 *
 * @see WithHqUser
 */
final class WithHqUserSecurityContextFactory implements WithSecurityContextFactory<WithHqUser> {

    @Override
    public SecurityContext createSecurityContext(WithHqUser withUser) {
        String username = StringUtils.hasLength(withUser.username()) ? withUser.username() : withUser.value();
        Assert.notNull(username, () -> withUser + " cannot have null username on both username and value properties");
        HqUserDetailsBean principal = new HqUserDetailsBean(withUser.domain(), withUser.domains(), username,
                withUser.isSuperUser(), withUser.enabledToggles(), withUser.enabledPreviews());
        Authentication authentication = new PreAuthenticatedAuthenticationToken(principal,
                "sessionId", principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }

}
