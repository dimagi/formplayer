package objects;

import org.springframework.data.redis.core.ValueOperations;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import session.FormSession;

/**
 * Redis cache record object for
 *
 * @author Clayton Sims (csims@dimagi.com)
 */
public class FormVolatilityRecord implements Serializable {
    //If the data dict structure is changed, bump the version of the key
    public final static String VOLATILITY_KEY_TEMPLATE = "FormInit-%s-%s-v2";

    private String key;
    private long timeout;
    private String entityName;

    private String username;
    private long openedOn;

    private String currentMessage;

    /**For serialization only **/
    public FormVolatilityRecord() {

    }

    public FormVolatilityRecord(String key, long timeout, String entityName) {
        this.key = key;
        this.timeout = timeout;
        this.entityName = entityName;
    }

    public String getKey() {
        return key;
    }

    //Crud Methods

    public void setKey(String key) {
        this.key = key;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getOpenedOn() {
        return openedOn;
    }

    public void setOpenedOn(long openedOn) {
        this.openedOn = openedOn;
    }

    public String getCurrentMessage() {
        return currentMessage;
    }

    public void setCurrentMessage(String currentMessage) {
        this.currentMessage = currentMessage;
    }

    private String formatOpenedMessage(String userTitle) {
        String message = String.format(
                "Warning: This form was started recently for %s by %s %s",
                entityName == null? "the same record" : entityName,
                userTitle,
                "%s");
        return message;
    }

    public boolean matchesUser(FormSession session) {
        return this.username.equals(session.getUsername());
    }

    public void write(ValueOperations<String, FormVolatilityRecord> volatilityCache) {
        volatilityCache.set(key, this, this.timeout, TimeUnit.SECONDS);
    }

    /**
     * This record represents an Opened Form
     *
     * @param session
     */
    public void updateFormOpened(FormSession session) {
        this.username = session.getUsername();
        this.currentMessage = formatOpenedMessage(session.getUsername());
        this.openedOn = new Date().getTime();

    }

    public String formatWarningString() {
        String formatString;

        long current = new Date().getTime();
        long delta = (current - openedOn) / 1000;

        if(delta < 60) {
            formatString = String.format("%d Seconds ago", delta);
        } else {
            delta = delta / 60;
            formatString = String.format("%d Minutes ago", delta);
        }

        return String.format(currentMessage, formatString);
    }
}
