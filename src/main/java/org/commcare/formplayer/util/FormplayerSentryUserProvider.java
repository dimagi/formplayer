package org.commcare.formplayer.util;

import io.sentry.protocol.User;
import io.sentry.spring.SentryUserProvider;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
class FormplayerSentryUserProvider implements SentryUserProvider {
    public User provideUser() {
        User user = new User();
        HttpServletRequest request = RequestUtils.getCurrentRequest();
        if (request != null) {
            user.setIpAddress(request.getRemoteAddr());

            RequestUtils.getUserDetails().ifPresent(userDetails -> {
                user.setId(String.valueOf(userDetails.getDjangoUserId()));
                user.setUsername(userDetails.getUsername());
            });
        }

        return user;
    }
}
