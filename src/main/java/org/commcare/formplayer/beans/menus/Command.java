package org.commcare.formplayer.beans.menus;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.suite.model.SessionDatum;

/**
 * Created by willpride on 4/13/16.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Command {
    private int index;
    private String displayText;
    private String audioUri;
    private String imageUri;
    private NavIconState navigationState;
    private String badgeText;

    enum NavIconState {
        NEXT, JUMP
    }

    public NavIconState getNavigationState() {
        return navigationState;
    }

    public void setNavigationState(NavIconState navigatonState) {
        this.navigationState = navigatonState;
    }

    public Command() {
    }

    public Command(MenuDisplayable menuDisplayable, int index, SessionWrapper session, String badgeText) {
        super();
        this.setIndex(index);
        this.setDisplayText(menuDisplayable.getDisplayText(
                session.getEvaluationContextWithAccumulatedInstances(menuDisplayable.getCommandID(), menuDisplayable.getRawText())));
        this.setImageUri(menuDisplayable.getImageURI());
        this.setAudioUri(menuDisplayable.getAudioURI());
        this.setNavigationState(getIconState(menuDisplayable, session));
        this.setBadgeText(badgeText);
    }

    private NavIconState getIconState(MenuDisplayable menuDisplayable, CommCareSession session) {
        NavIconState iconChoice = NavIconState.NEXT;

        //figure out some icons
        if (menuDisplayable instanceof Entry) {
            SessionDatum datum = session.getNeededDatum((Entry)menuDisplayable);
            if (datum == null || !(datum instanceof EntityDatum)) {
                iconChoice = NavIconState.JUMP;
            }
        }
        return iconChoice;
    }

    public int getIndex() {
        return index;
    }

    private void setIndex(int index) {
        this.index = index;
    }

    public String getDisplayText() {
        return displayText;
    }

    private void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public String getAudioUri() {
        return audioUri;
    }

    private void setAudioUri(String audioUri) {
        this.audioUri = audioUri;
    }

    public String getImageUri() {
        return imageUri;
    }

    private void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public String getBadgeText() {
        return badgeText;
    }

    public void setBadgeText(String badgeText) {
        this.badgeText = badgeText;
    }

    @Override
    public String toString() {
        return "Command [index=" + index + ", text=" + displayText + ", " +
                "audioUri=" + audioUri + ", imageUri=" + imageUri + "]";
    }
}
