package application;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.ContentCachingResponseWrapper;
import util.FormplayerHttpRequest;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RequestResponseLoggingFilter extends GenericFilterBean {

    private final Log log = LogFactory.getLog(Application.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);
        filterChain.doFilter(request, responseWrapper);

        // prep the response for the actual outbound connection
        responseWrapper.copyBodyToResponse();

        // Get a copy of what we are returning for logging
        String requestBody = IOUtils.toString(httpRequest.getReader());
        String responseBody = new String(responseWrapper.getContentAsByteArray());

        if (request instanceof FormplayerHttpRequest) {
            FormplayerHttpRequest formplayerHttpRequest = (FormplayerHttpRequest) request;
            String domain = formplayerHttpRequest.getDomain();
            String url = new String(formplayerHttpRequest.getRequestURL());
            String user = formplayerHttpRequest.getUserDetails().getUsername();
        }

    }

}
