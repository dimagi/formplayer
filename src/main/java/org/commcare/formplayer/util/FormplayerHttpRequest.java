package org.commcare.formplayer.util;

import org.commcare.formplayer.beans.auth.HqUserDetailsBean;

import javax.servlet.http.HttpServletRequest;

public class FormplayerHttpRequest extends MultipleReadHttpRequest {
    private HqUserDetailsBean userDetails;
    private String domain;
    private boolean requestValidatedWithHMAC;

    public FormplayerHttpRequest(HttpServletRequest request) {
        super(request);
    }

    public void setUserDetails(HqUserDetailsBean userDetails) {
        this.userDetails = userDetails;
    }

    public HqUserDetailsBean getUserDetails() {
        return userDetails;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }

    public void setRequestValidatedWithHMAC(boolean requestValidatedWithHMAC) {
        this.requestValidatedWithHMAC = requestValidatedWithHMAC;
    }

    public boolean getRequestValidatedWithHMAC() {
        return requestValidatedWithHMAC;
    }
}
