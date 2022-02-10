package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Request to change the locale of the form session
 * <p>
 * Created by willpride on 1/20/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeLocaleRequestBean extends SessionRequestBean {
    private String locale;

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public ChangeLocaleRequestBean() {
    }


    @Override
    public String toString() {
        return "ChangeLocaleRequestBean [locale=" + locale + ", sessionId=" + sessionId + "]";
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
