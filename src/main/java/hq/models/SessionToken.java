package hq.models;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import javax.xml.bind.DatatypeConverter;
import java.util.Date;

/**
 * Represents the user's session. This is set by HQ.
 */
public class SessionToken {
    private String sessionId;
    private Date expireDate;
    private JSONObject sessionData;

    /**
     * Parses base64 sessionData into a JSONObject. The decoded session data is of the format:
     *
     *      "<unique_id>:{...session data...}"
     *
     * @param base64SessionData
     */
    public void parseSessionData(String base64SessionData) {
        byte[] data = DatatypeConverter.parseBase64Binary(base64SessionData);
        String jsonData = StringUtils.substringAfter(new String(data), ":");
        this.sessionData = new JSONObject(jsonData);
    }

    public int getUserId() {
        if (this.sessionData.has("_auth_user_id")) {
            return this.sessionData.getInt("_auth_user_id");
        } else {
            return -1;
        }
    }

    public String getSessionid() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = expireDate;
    }
}
