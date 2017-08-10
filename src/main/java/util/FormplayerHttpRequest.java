package util;

import beans.auth.HqUserDetailsBean;

import javax.servlet.http.HttpServletRequest;

public class FormplayerHttpRequest extends MultipleReadHttpRequest {
    private HqUserDetailsBean userDetails;
    private String domain;

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
}
