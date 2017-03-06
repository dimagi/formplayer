package objects;

import com.fasterxml.jackson.annotation.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
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
    private final Map<String, String> data = new HashMap<>();
    private Map<String, String> userData = new HashMap<>();
    private Map<String, FunctionHandler[]> functionContext = new HashMap<>();
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

    @JsonAnyGetter
    public Map<String, String> getData() {
        return data;
    }

    @JsonAnySetter
    public void setData(String key, Object value) {
        data.put(key, value.toString());
    }

    public String get(String name) {
        return data.get(name);
    }

    @Override
    public String toString(){
        return "SessionData: [username=" + username + ", domain=" + domain
                + ", data=" + data + "]";
    }

    @JsonGetter(value = "user_data")
    public Map<String, String> getUserData() {
        return userData;
    }
    @JsonSetter(value="user_data")
    public void setUserData(Map<String, String> userData) {
        this.userData = userData;
    }

    @JsonGetter(value="function_context")
    public Map<String, FunctionHandler[]> getFunctionContext() {
        return functionContext;
    }
    @JsonSetter(value="function_context")
    public void setFunctionContext(Map<String, FunctionHandler[]> functionContext) {
        this.functionContext = functionContext;
    }
}
