package services;

import beans.auth.HqUserDetailsBean;

public interface HqUserDetailsService {
    HqUserDetailsBean getUserDetails(String domain, String sessionKey);
}
