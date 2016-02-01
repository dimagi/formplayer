package objects;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Map;

/**
 * Created by willpride on 1/20/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionData {

    private String username;
    private Map<String, String> additionalFilters;
    private String domain;
    private String userId;
    private String appId;
    private Map<String, String> userData;
    private Map<String, String> data;
    private String host;
    private String sessionName;
    private String appVersion;
    private String deviceId;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    @JsonGetter(value = "additional_filters")
    public Map<String, String> getAdditionalFilters() {
        return additionalFilters;
    }
    @JsonSetter(value = "additional_filters")
    public void setAdditionalFilters(Map<String, String> additionalFilters) {
        this.additionalFilters = additionalFilters;
    }

    public String getDomain() {
        return domain;
    }
    public void setDomain(String domain) {
        this.domain = domain;
    }
    @JsonGetter(value = "user_id")
    public String getUserId() {
        return userId;
    }
    @JsonSetter(value = "user_id")
    public void setUserId(String userId) {
        this.userId = userId;
    }
    @JsonGetter(value = "user_data")
    public Map<String, String> getUserData() {
        return userData;
    }
    @JsonSetter(value = "user_data")
    public void setUserData(Map<String, String> userData) {
        this.userData = userData;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
    @JsonGetter(value = "session_name")
    public String getSessionName() {
        return sessionName;
    }
    @JsonSetter(value = "session_name")
    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }
    @JsonGetter(value = "app_version")
    public String getAppVersion() {
        return appVersion;
    }
    @JsonSetter(value = "app_version")
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }
    @JsonGetter(value = "device_id")
    public String getDeviceId() {
        return deviceId;
    }
    @JsonSetter(value = "device_id")
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    @JsonGetter(value = "app_id")
    public String getAppId() {
        return appId;
    }
    @JsonSetter(value = "app_id")
    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}
