package org.commcare.formplayer.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commcare.formplayer.auth.UserDomainPreAuthPrincipal;
import org.commcare.formplayer.beans.auth.HqSessionKeyBean;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.exceptions.SessionAuthUnavailableException;
import org.commcare.formplayer.exceptions.UserDetailsException;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class HqUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
    @Value("${commcarehq.host}")
    private String host;

    @Value("${commcarehq.formplayerAuthKey}")
    private String formplayerAuthKey;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    public HqUserDetailsBean getUserDetails(String domain, String sessionKey) {
        HttpHeaders headers = new HttpHeaders();
        String data = null;
        try {
            data = objectMapper.writeValueAsString(new HqSessionKeyBean(domain, sessionKey));
            headers.set("X-MAC-DIGEST", getHmac(data));
        } catch (Exception e) {
            throw new UserDetailsException(e);
        }
        HttpEntity<String> request = new HttpEntity<>(data, headers);

        try {
            HqUserDetailsBean userDetails = restTemplate.postForObject(getSessionDetailsUrl(), request, HqUserDetailsBean.class);
            userDetails.setDomain(domain);
            return userDetails;
        } catch(HttpClientErrorException.NotFound nfe) {
            throw new SessionAuthUnavailableException();
        }
    }

    private String getSessionDetailsUrl() {
        return host + Constants.SESSION_DETAILS_VIEW;
    }

    private String getHmac(String data) throws Exception {
        return RequestUtils.getHmac(formplayerAuthKey, data);
    }

    /**
     * Called during the authentication workflow to authenticate the user.
     * @return UserDetails object
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        final UserDomainPreAuthPrincipal principal = (UserDomainPreAuthPrincipal) token.getPrincipal();
        final String sessionId = (String) token.getCredentials();
        try {
            HqUserDetailsBean userDetails = getUserDetails(principal.getDomain(), sessionId);
            if (!userDetails.isAuthorized(principal.getDomain(), principal.getUsername())) {
                throw new UsernameNotFoundException("Unable to authenticate user in requested domain");
            }
            return userDetails;
        } catch (SessionAuthUnavailableException e) {
            throw new UsernameNotFoundException("Unable to authenticate user");
        }
    }
}
