package hq.models;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import javax.xml.bind.DatatypeConverter;
import java.util.Date;

/**
 * Created by benrudolph on 9/7/16.
 */
public class SessionToken {
    private String sessionId;
    private Date expireDate;
    private JSONObject sessionData;

    public void parseSessionData(String base64SessionData) {
        byte[] data = DatatypeConverter.parseBase64Binary(base64SessionData);
        String jsonData = StringUtils.substringAfter(new String(data), ":");
        this.sessionData = new JSONObject(jsonData);
    }

    public int getUserId() {
        return this.sessionData.getInt("_auth_user_id");
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
