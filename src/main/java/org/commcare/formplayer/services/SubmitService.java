package org.commcare.formplayer.services;

import lombok.extern.apachecommons.CommonsLog;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.SimpleTimer;
import org.commcare.formplayer.web.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Service that handles form submission to CommCareHQ
 */
@Service
@RequestScope
@CommonsLog
public class SubmitService {

    @Autowired
    RestoreFactory restoreFactory;

    @Autowired
    private CategoryTimingHelper categoryTimingHelper;

    @Autowired
    private WebClient webClient;

    private CategoryTimingHelper.RecordingTimer submitTimer;

    public String submitForm(String formXml, String submitUrl) {
        submitTimer = categoryTimingHelper.newTimer(Constants.TimingCategories.SUBMIT_FORM_TO_HQ, restoreFactory.getDomain());
        submitTimer.start();
        try {
            return webClient.post(submitUrl, formXml);
        } finally {
            submitTimer.end().record();
        }
    }

    public SimpleTimer getSubmitTimer() {
        return submitTimer;
    }
}
