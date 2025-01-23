package org.commcare.formplayer.request;

import org.commcare.formplayer.filters.FilterOrder;
import org.commcare.formplayer.util.MultipleReadHttpRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Wrap all requests in this request wrapper to allow us to read the body multiple times.
 * This is necessary since some of the authentication details are contained in the body.
 */
@Component
@Order(FilterOrder.FIRST)
public class MultipleReadRequestWrappingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        filterChain.doFilter(new MultipleReadHttpRequest(req), response);
    }

}
