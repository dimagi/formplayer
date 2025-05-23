package org.commcare.formplayer.application;

import io.sentry.SentryLevel;

import org.commcare.formplayer.annotations.ConfigureStorageFromSession;
import org.commcare.formplayer.annotations.UserLock;
import org.commcare.formplayer.annotations.UserRestore;
import org.commcare.formplayer.beans.SubmitRequestBean;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.services.CategoryTimingHelper;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerSentry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller class (API endpoint) containing all form entry logic. This includes
 * opening a new form, question answering, and form submission.
 */
@RestController
@EnableAutoConfiguration
public class FormSubmissionController extends AbstractBaseController {

    @Autowired
    private FormSubmissionHelper formSubmissionHelper;

    @Autowired
    private CategoryTimingHelper categoryTimingHelper;

    @RequestMapping(value = Constants.URL_SUBMIT_FORM, method = RequestMethod.POST)
    @ResponseBody
    @UserLock
    @UserRestore
    @ConfigureStorageFromSession
    public SubmitResponseBean submitForm(@RequestBody SubmitRequestBean submitRequestBean,
            @CookieValue(name = Constants.POSTGRES_DJANGO_SESSION_ID, required = false) String authToken,
            HttpServletRequest request) throws Exception {
            CategoryTimingHelper.RecordingTimer timer = categoryTimingHelper.newTimer(Constants.TimingCategories.PROCESS_AND_SUBMIT_FORM, submitRequestBean.getDomain());
            timer.start();
            try {
                return formSubmissionHelper.processAndSubmitForm(request, submitRequestBean.getSessionId(), submitRequestBean.getDomain(),
                submitRequestBean.isPrevalidated(), submitRequestBean.getAnswers(), submitRequestBean.getWindowWidth());
            } finally {
                timer.end().record();
                int timingThreshold = 5000;
                if (timer.durationInMs() > timingThreshold) {
                    Exception e = new Exception("Form submission took longer than " + timingThreshold + "ms");
                    FormplayerSentry.captureException(e, SentryLevel.WARNING);
                };
            }
    }
}
