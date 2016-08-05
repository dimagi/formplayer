package application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import repo.SessionRepo;
import services.RestoreService;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Base Controller class containing exception handling logic and
 * autowired beans used in both MenuController and FormController
 */
public abstract class AbstractBaseController {

    @Autowired
    protected RestoreService restoreService;

    @Autowired
    protected SessionRepo sessionRepo;

    @Autowired
    private HtmlEmail exceptionMessage;

    private final Log log = LogFactory.getLog(AbstractBaseController.class);

    @ExceptionHandler(Exception.class)
    public String handleError(HttpServletRequest req, Exception exception) {
        log.error("Request: " + req.getRequestURL() + " raised " + exception);
        exception.printStackTrace();
        try {
            sendExceptionEmail(exception);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Unable to send email");
        }
        JSONObject errorReturn = new JSONObject();
        errorReturn.put("exception", exception);
        errorReturn.put("url", req.getRequestURL());
        errorReturn.put("status", "error");
        return errorReturn.toString();
    }

    private void sendExceptionEmail(Exception exception) {
        try {
            exceptionMessage.setHtmlMsg(getExceptionEmailBody(exception));
            exceptionMessage.setSubject("Formplayer Menu Exception: " + exception.getMessage());
            exceptionMessage.send();
        } catch(EmailException e){
            // I think we should fail quietly on this
            log.error("Couldn't send exception email: " + e);
        }
    }


    private String getExceptionEmailBody(Exception exception){
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String formattedTime = dateFormat.format(new Date());
        String[] stackTrace = ExceptionUtils.getStackTrace(exception).split("\n");
        String stackTraceHTML = StringUtils.replace(
            StringUtils.join(stackTrace, "<br />"), "\t", "&nbsp;&nbsp;&nbsp;"
        );
        return "<html>" +
                "<h3>Message</h3>" +
                "<p>" + exception.getMessage() + "</p>" +
                "<h3>Time</h3>" +
                "<p>" + formattedTime + "<p>" +
                "<h3>Trace</h3>" +
                "<p>" + stackTraceHTML + "</p>" +
                "</html>";
    }
}
