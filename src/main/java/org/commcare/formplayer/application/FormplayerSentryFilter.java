package org.commcare.formplayer.application;

import io.sentry.Sentry;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerHttpRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class FormplayerSentryFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        configureSentryScope(req);
        filterChain.doFilter(req, response);
    }

    private void configureSentryScope(HttpServletRequest request) {
        Sentry.configureScope(scope -> {
            scope.setTag(Constants.URI, request.getRequestURI());
            if (request instanceof FormplayerHttpRequest) {
                String domain = ((FormplayerHttpRequest) request).getDomain();
                if (domain != null) {
                    scope.setTag(Constants.DOMAIN_TAG, domain);
                }
            }
        });
    }
}
