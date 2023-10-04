package org.commcare.formplayer.application;

import org.commcare.formplayer.filters.FilterOrder;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.RequestUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.sentry.Sentry;

@Component
@Order(FilterOrder.SENTRY)
public class FormplayerSentryFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        configureSentryScope(req);
        filterChain.doFilter(req, response);
    }

    private void configureSentryScope(HttpServletRequest request) {
        Sentry.configureScope(scope -> {
            scope.setTag(Constants.URI, request.getRequestURI());
            RequestUtils.getUserDetails().ifPresent(userDetails -> {
                scope.setTag(Constants.DOMAIN_TAG, userDetails.getDomain());
            });
        });
    }
}
