package org.commcare.formplayer.util;

import org.springframework.stereotype.Component;

import io.sentry.spring.jakarta.SentryUserProvider;
import jakarta.servlet.http.HttpServletRequest;

import io.sentry.protocol.User;


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
