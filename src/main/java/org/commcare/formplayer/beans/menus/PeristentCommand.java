package org.commcare.formplayer.beans.menus;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.checkerframework.checker.units.qual.A;
import org.commcare.formplayer.beans.menus.Command.NavIconState;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.Entry;

import java.util.ArrayList;

import lombok.EqualsAndHashCode;

/**
 * Model used to represent a persistent menu in FP HTTP API
 */
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PeristentCommand {

    private String index;
    private String displayText;
    private NavIconState navigationState;
    private String imageUri;
    private ArrayList<PeristentCommand> commands = new ArrayList<>();

    /**
     * serialization only
     */
    public PeristentCommand() {
    }

    public PeristentCommand(String index, String displayText,
        MenuDisplayable menuDisplayable, SessionWrapper session) {
        this.index = index;
        this.displayText = displayText;
        this.setImageUri(menuDisplayable.getImageURI());
        this.setNavigationState(getIconState(menuDisplayable, session));
    }

    public PeristentCommand(String index, String displayText) {
        this.index = index;
        this.displayText = displayText;
    }

    public String getIndex() {
        return index;
    }

    public String getDisplayText() {
        return displayText;
    }

    public ArrayList<PeristentCommand> getCommands() {
        return commands;
    }

    public void addCommand(PeristentCommand command) {
        commands.add(command);
    }

    public NavIconState getNavigationState() {
        return navigationState;
    }

    public void setNavigationState(NavIconState navigatonState) {
        this.navigationState = navigatonState;
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

    public String getImageUri() {
        return imageUri;
    }

    private void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }
}
