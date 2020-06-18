package application;

import beans.auth.HqUserDetailsBean;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestResponseLoggingFilter extends GenericFilterBean {

    private Log log = LogFactory.getLog(RequestResponseLoggingFilter.class);

    public RequestResponseLoggingFilter(Log log){
        super();
        if (log != null) {
            this.log = log;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);
        filterChain.doFilter(request, responseWrapper);

        String requestBody = IOUtils.toString(httpRequest.getReader());
        String responseBody = new String(responseWrapper.getContentAsByteArray());

        String restoreAs = null;
        String url = null;
        String domain = null;
        String user = null;

        try {
            restoreAs = new JSONObject(requestBody).getString("restoreAs");
        } catch (JSONException e) {
            // Swallow, restoreAs not always provided in request.
        }

        if (request instanceof FormplayerHttpRequest) {
            FormplayerHttpRequest formplayerHttpRequest = (FormplayerHttpRequest) request;
            domain = formplayerHttpRequest.getDomain();
            url = new String(formplayerHttpRequest.getRequestURL());

            HqUserDetailsBean userDetailsBean = formplayerHttpRequest.getUserDetails();
            user = (userDetailsBean != null) ? formplayerHttpRequest.getUserDetails().getUsername() : null;
        }

        JSONObject logLine = (new JSONObject())
                .put("requestUrl", url)
                .put("requestBody", requestBody)
                .put("projectSpace", domain)
                .put("username", user)
                .put("restoreAs", restoreAs)
                .put("date", this.currentTimeISO8601())
                .put("responseBody", responseBody);
        log.info(logLine);

        // prep the response for the actual outbound connection
        responseWrapper.copyBodyToResponse();
    }

    private String currentTimeISO8601() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        return df.format(new Date());
    }

}
