package beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Request for Formplayer to perform a case migration
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MigrationRequestBean extends AuthenticatedRequestBean {
    private String[] caseIds;
    private String formXml;
    private String postUrl;
    private String appId;

    public String[] getCaseIds() {
        return caseIds;
    }

    public void setCaseIds(String[] caseIds) {
        this.caseIds = caseIds;
    }

    public String getFormXml() {
        return formXml;
    }

    public void setFormXml(String formXml) {
        this.formXml = formXml;
    }

    public String getPostUrl() {
        return postUrl;
    }

    public void setPostUrl(String postUrl) {
        this.postUrl = postUrl;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }
}
