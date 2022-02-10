package org.commcare.formplayer.objects;

import org.commcare.formplayer.beans.NotificationMessage;
import org.commcare.formplayer.session.FormSession;
import org.commcare.formplayer.util.UserUtils;
import org.springframework.data.redis.core.ValueOperations;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Redis cache record object for managing records of actions (open, complete, etc) which
 * are taken against forms that are marked as 'volatile.'
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
    private long submittedOn = 0;

    private String currentMessage;

    /**
     * For serialization only
     **/
    public FormVolatilityRecord() {

    }

    public FormVolatilityRecord(String key, long timeout, String entityName) {
        this.key = key;
        setTimeout(timeout);
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
        //Only allow notification records for up to an hour
        this.timeout = Math.min(timeout, 60 * 60);
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

    public long getSubmittedOn() {
        return submittedOn;
    }

    public void setSubmittedOn(long submittedOn) {
        this.submittedOn = submittedOn;
    }

    private String formatOpenedMessage(String userTitle) {
        String message = String.format(
                "Warning: %s recently started this form for %s",
                userTitle,
                entityName == null ? "the same record" : entityName);
        return message;
    }

    private String formatSubmittedMessage(String userTitle) {
        String message = String.format(
                "Warning: %s recently submitted this form for %s",
                userTitle,
                entityName == null ? "the same record" : entityName);
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
     */
    public void updateFormOpened(FormSession session) {
        this.username = session.getUsername();
        this.currentMessage = formatOpenedMessage(UserUtils.getUsernameBeforeAtSymbol(session.getUsername()));
        this.openedOn = new Date().getTime();
    }


    /**
     * This record represents a finished form
     */
    public void updateFormSubmitted(FormSession session) {
        this.username = session.getUsername();
        this.currentMessage = formatSubmittedMessage(UserUtils.getUsernameBeforeAtSymbol(session.getUsername()));
        this.submittedOn = new Date().getTime();
    }

    public NotificationMessage getNotificationIfRelevant(long lastSyncTime) {
        String formatString;
        NotificationMessage.Type type = NotificationMessage.Type.warning;

        long anchor = wasSubmitted() ? submittedOn : openedOn;

        long current = new Date().getTime();
        long delta = (current - anchor) / 1000;

        if (delta < 60) {
            formatString = String.format(" %d Seconds ago", delta);
        } else {
            delta = delta / 60;
            formatString = String.format(" %d Minutes ago", delta);
        }

        if (submittedOn > lastSyncTime) {
            formatString += ". Your data may be out of date! " +
                    "You haven't synced since this form was submitted";
        }
        if (wasSubmitted() && submittedOn < lastSyncTime) {
            return null;
        }

        return new NotificationMessage(currentMessage + formatString, type, NotificationMessage.Tag.volatility);
    }

    public boolean wasSubmitted() {
        return submittedOn != 0;
    }
}
