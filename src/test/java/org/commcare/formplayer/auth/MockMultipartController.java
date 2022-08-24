package org.commcare.formplayer.auth;

import static org.commcare.formplayer.util.Constants.PART_ANSWER;
import static org.commcare.formplayer.util.Constants.PART_FILE;

import org.commcare.formplayer.annotations.ConfigureStorageFromSession;
import org.commcare.formplayer.annotations.UserLock;
import org.commcare.formplayer.annotations.UserRestore;
import org.commcare.formplayer.beans.AnswerQuestionRequestBean;
import org.commcare.formplayer.beans.FormEntryResponseBean;
import org.commcare.formplayer.beans.SessionRequestBean;
import org.commcare.formplayer.util.Constants;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Mock Controller to test auth for multipart requests
 */
@RestController
@EnableAutoConfiguration
public class MockMultipartController {

    @RequestMapping(
            value = "mock_mulipart_request",
            method = RequestMethod.POST,
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}
    )
    @UserLock
    @UserRestore
    @ConfigureStorageFromSession
    public void multipartRequest(
            @RequestPart(PART_FILE) MultipartFile file,
            @RequestPart(PART_ANSWER) SessionRequestBean sessionRequestBean,
            @CookieValue(name = Constants.POSTGRES_DJANGO_SESSION_ID, required = false) String authToken) {
        return;
    }
}
