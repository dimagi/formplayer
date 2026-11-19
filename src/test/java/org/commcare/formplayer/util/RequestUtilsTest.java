package org.commcare.formplayer.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

public class RequestUtilsTest {

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void getUserDetails_principalIsNotAnHqUser_isEmpty() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                new User("someone", "pw", AuthorityUtils.NO_AUTHORITIES), "pw"));
        SecurityContextHolder.setContext(context);

        assertTrue(RequestUtils.getUserDetails().isEmpty());
        assertFalse(RequestUtils.isPublicSession());
    }

    @Test
    public void getUserDetails_anonymousAuthentication_isEmpty() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        SecurityContextHolder.setContext(context);

        assertTrue(RequestUtils.getUserDetails().isEmpty());
        assertFalse(RequestUtils.isPublicSession());
    }

    @Test
    public void getUserDetails_noAuthentication_isEmpty() {
        SecurityContextHolder.clearContext();

        assertTrue(RequestUtils.getUserDetails().isEmpty());
        assertFalse(RequestUtils.isPublicSession());
    }
}
