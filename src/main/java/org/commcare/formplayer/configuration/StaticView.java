package org.commcare.formplayer.configuration;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.apachecommons.CommonsLog;

/**
 * Simple custom error view to replace the default whitelabel error view.
 * Modelled after org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration.StaticView
 */
@Component("error")
@CommonsLog
public class StaticView implements View {

    @Override
    public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        if (response.isCommitted()) {
            String message = getMessage(model);
            log.error(message);
            return;
        }

        String builder = htmlEscape(model.get("error")) + "(" + model.get("status") + ") : " +
                model.get("message");
        response.getWriter().append(builder);
    }

    private String htmlEscape(Object input) {
        return (input != null) ? HtmlUtils.htmlEscape(input.toString()) : null;
    }

    private String getMessage(Map<String, ?> model) {
        Object path = model.get("path");
        String message = "Cannot render error page for request [" + path + "]";
        if (model.get("message") != null) {
            message += " and exception [" + model.get("message") + "]";
        }
        message += " as the response has already been committed.";
        message += " As a result, the response may have the wrong status code.";
        return message;
    }

    @Override
    public String getContentType() {
        return "text/html";
    }
}
