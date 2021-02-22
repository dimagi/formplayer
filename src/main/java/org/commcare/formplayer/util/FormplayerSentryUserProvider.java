package org.commcare.formplayer.util;

import io.sentry.protocol.User;
import io.sentry.spring.SentryUserProvider;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.springframework.stereotype.Component;

@Component
class FormplayerSentryUserProvider implements SentryUserProvider {
    public User provideUser() {
        User user = new User();
        FormplayerHttpRequest request = RequestUtils.getCurrentRequest();
        if (request != null) {
            HqUserDetailsBean userDetails = request.getUserDetails();
            if (userDetails != null) {
                user.setId(String.valueOf(userDetails.getDjangoUserId()));
                user.setUsername(userDetails.getUsername());
                user.setIpAddress(request.getRemoteAddr());
            }
        }

        return user;
    }
}
