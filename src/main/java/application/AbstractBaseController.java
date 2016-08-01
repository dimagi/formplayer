package application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.bind.annotation.ExceptionHandler;
import repo.FormSessionRepo;
import repo.MenuSessionRepo;
import services.InstallService;
import services.RestoreService;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Base Controller class containing exception handling logic and
 * autowired beans used in both MenuController and FormController
 */
public abstract class AbstractBaseController {

    @Autowired
    protected RestoreService restoreService;

    @Autowired
    protected FormSessionRepo formSessionRepo;

    @Autowired
    protected MenuSessionRepo menuSessionRepo;

    @Autowired
    protected InstallService installService;

    @Autowired
    private JavaMailSenderImpl exceptionSender;

    @Autowired
    private SimpleMailMessage exceptionMessage;

    private final Log log = LogFactory.getLog(AbstractBaseController.class);

    @ExceptionHandler(Exception.class)
    public String handleError(HttpServletRequest req, Exception exception) {
        log.error("Request: " + req.getRequestURL() + " raised " + exception);
        exception.printStackTrace();
        sendExceptionEmail(exception);
        JSONObject errorReturn = new JSONObject();
        errorReturn.put("exception", exception);
        errorReturn.put("url", req.getRequestURL());
        errorReturn.put("status", "error");
        return errorReturn.toString();
    }

    private void sendExceptionEmail(Exception exception) {
        exceptionMessage.setText(getExceptionEmailBody(exception));
        exceptionMessage.setSubject("Formplayer Menu Exception: " + exception.getMessage());
        try {
            exceptionSender.send(exceptionMessage);
        } catch(MailSendException e){
            // I think we should fail quietly on this
            log.error("Couldn't send exception email: " + e);
        }
    }


    private String getExceptionEmailBody(Exception exception){
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String formattedTime = dateFormat.format(new Date());
        return "Message: " + exception.getMessage() + " \n " +
                "Time : " + formattedTime + " \n " +
                "Trace: " + Arrays.toString(exception.getStackTrace());
    }
}
