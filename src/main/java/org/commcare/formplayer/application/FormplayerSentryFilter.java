package org.commcare.formplayer.application;

import io.sentry.Sentry;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.util.Constants;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(1)
public class FormplayerSentryFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        configureSentryScope(req);
        filterChain.doFilter(req, response);
    }

    private void configureSentryScope(HttpServletRequest request) {
        Sentry.configureScope(scope -> {
            scope.setTag(Constants.URI, request.getRequestURI());
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                HqUserDetailsBean userDetails = (HqUserDetailsBean) authentication.getPrincipal();
                scope.setTag(Constants.DOMAIN_TAG, userDetails.getDomain());
            }
        });
    }
}
