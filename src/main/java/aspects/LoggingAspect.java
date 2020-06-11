package aspects;

import beans.AuthenticatedRequestBean;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RequestMapping;
import util.FormplayerSentry;


import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;

/**
 * Aspect to log the inputs and return of each API method
 */
@Aspect
@Order(4)
public class LoggingAspect {

    private final Log log = LogFactory.getLog(LoggingAspect.class);

    @Autowired
    private FormplayerSentry raven;

    @Around(value = "@annotation(org.springframework.web.bind.annotation.RequestMapping) " +
            "&& !@annotation(annotations.NoLogging)")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        final String requestPath = this.getRequestPath(joinPoint);
        final Object requestBean = this.getRequestBean(joinPoint);
        final String requestBody = this.getRequestBody(joinPoint);
        final AuthenticatedRequestBean authenticatedRequestBean =
                (requestBean != null && requestBean instanceof AuthenticatedRequestBean) ?
                        (AuthenticatedRequestBean) requestBean : null;

        String domain = null;
        String username = null;
        String restoreAs = null;

        if (authenticatedRequestBean != null) {
            domain = authenticatedRequestBean.getDomain();
            username = authenticatedRequestBean.getUsername();
            restoreAs = authenticatedRequestBean.getRestoreAs();
            String bean = authenticatedRequestBean.toString();

            raven.newBreadcrumb()
                    .setData(
                            "path", requestPath,
                            "domain", domain,
                            "username", username,
                            "restoreAs", restoreAs,
                            "bean", bean
                    )
                    .record();
        }

        Object result = joinPoint.proceed();

        JSONObject logLine = (new JSONObject())
                .put("request_path", requestPath)
                .put("request_body", requestBody)
                .put("project_space", domain)
                .put("username", username)
                .put("restore_as", restoreAs)
                // TODO: Figure out a way to get response JSON.
                .put("responseBean", result);
        log.info(logLine);

        return result;
    }

    private String getRequestPath(ProceedingJoinPoint joinPoint) {
        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Method m = ms.getMethod();
        return m.getAnnotation(RequestMapping.class).value()[0]; //Should be only one
    }

    private Object getRequestBean(ProceedingJoinPoint joinPoint) {
        Object requestBean = null;
        try {
            requestBean = joinPoint.getArgs()[0];
        } catch(ArrayIndexOutOfBoundsException e) {
            // ignore
        }
        return requestBean;
    }

    private String getRequestBody(ProceedingJoinPoint joinPoint) {
        String requestBody = null;
        try {
            Object[] joinPointArgs = joinPoint.getArgs();
            if (joinPointArgs.length > 0) {
                if (joinPointArgs[joinPointArgs.length - 1] instanceof HttpServletRequest) {
                    HttpServletRequest httpServletRequest = (HttpServletRequest) joinPointArgs[joinPointArgs.length - 1];
                    // TODO: Make JSON Object instead of JSON string.
                    requestBody = IOUtils.toString(httpServletRequest.getReader());
                }
            }
        } catch(Exception e) {
            // Swallow all exceptions so we don't fail
        }
        return requestBody;
    }

}
