package org.commcare.formplayer.beans.menus;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.checkerframework.checker.units.qual.A;
import org.commcare.formplayer.beans.menus.CommandUtils.NavIconState;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.suite.model.MenuDisplayable;

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
        this.setNavigationState(CommandUtils.getIconState(menuDisplayable, session));
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

    public String getImageUri() {
        return imageUri;
    }

    private void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }
}
