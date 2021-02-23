package org.commcare.formplayer.util;

import io.sentry.protocol.User;
import io.sentry.spring.SentryUserProvider;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
class FormplayerSentryUserProvider implements SentryUserProvider {
    public User provideUser() {
        User user = new User();
        HttpServletRequest request = RequestUtils.getCurrentRequest();
        if (request != null) {
            user.setIpAddress(request.getRemoteAddr());

            if (request instanceof FormplayerHttpRequest) {
                FormplayerHttpRequest formplayerRequest = (FormplayerHttpRequest) request;
                HqUserDetailsBean userDetails = formplayerRequest.getUserDetails();
                if (userDetails != null) {
                    user.setId(String.valueOf(userDetails.getDjangoUserId()));
                    user.setUsername(userDetails.getUsername());
                }
            }
        }

        return user;
    }
}
