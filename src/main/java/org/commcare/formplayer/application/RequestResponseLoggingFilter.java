package org.commcare.formplayer.application;

import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.commcare.formplayer.util.FormplayerHttpRequest;

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

    /**
     * Wraps the filterChain `doFilter` call by logging the request and response. Catches potential
     * exceptions within the method and does not propagate them so the request is unaffected.
     * @param request request object
     * @param response response object
     * @param filterChain filter chain where `doFilter` is called
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        // json corresponding to the log ine and passed around to log as much as possible in case of exceptions.
        JSONObject logLineJson = new JSONObject();

        try {
            // Extract information from request before moving to the next filter along the chain.
            this.doBefore(logLineJson, request);
        } catch (Exception e) {
            log.error(e);
            logLineJson.put("loggingRequestError", e);
        }

        // Pass onto next filter, which should not throw an exception.
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);
        filterChain.doFilter(request, responseWrapper);

        try {
            // Extract information from response.
            this.doAfter(logLineJson, responseWrapper);
        } catch (Exception e) {
            log.error(e);
            logLineJson.put("loggingResponseError", e);
        } finally {
            // Always log and always prep the response for outbound connection
            log.info(logLineJson);
            responseWrapper.copyBodyToResponse();
        }
    }

    private void doBefore(JSONObject logLineJson,
                          ServletRequest request) throws IOException {
        final HttpServletRequest httpRequest = (HttpServletRequest) request;

        String requestBody = IOUtils.toString(httpRequest.getReader());
        logLineJson.put("requestBody", requestBody);

        try {
            logLineJson.put("restoreAs", new JSONObject(requestBody).getString("restoreAs"));
        } catch (JSONException e) {
            // Swallow, restoreAs not always provided in request.
        }

        FormplayerHttpRequest formplayerHttpRequest = (FormplayerHttpRequest) request;
        logLineJson.put("projectSpace", formplayerHttpRequest.getDomain());
        logLineJson.put("requestUrl", new String(formplayerHttpRequest.getRequestURL()));

        HqUserDetailsBean userDetailsBean = formplayerHttpRequest.getUserDetails();
        String user = (userDetailsBean != null) ? formplayerHttpRequest.getUserDetails().getUsername() : null;
        logLineJson.put("username" , user);
    }

    private void doAfter(JSONObject logLineJson, ContentCachingResponseWrapper responseWrapper) {
        String responseBody = new String(responseWrapper.getContentAsByteArray());
        logLineJson.put("date", this.currentTimeISO8601())
                .put("responseBody", responseBody);
    }

    private String currentTimeISO8601() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        return df.format(new Date());
    }

}
