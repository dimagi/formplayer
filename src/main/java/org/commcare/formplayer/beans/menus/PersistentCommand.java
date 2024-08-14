package org.commcare.formplayer.beans.menus;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.commcare.formplayer.beans.menus.CommandUtils.NavIconState;

import java.util.ArrayList;

import lombok.EqualsAndHashCode;

/**
 * Model used to represent a persistent menu in FP HTTP API
 */
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersistentCommand {

    private String index;
    private String displayText;
    private NavIconState navigationState;
    private String imageUri;
    private ArrayList<PersistentCommand> commands = new ArrayList<>();

    /**
     * serialization only
     */
    public PersistentCommand() {
    }

    public PersistentCommand(String index, String displayText,
        String imageUri, NavIconState navigationState) {
        this.index = index;
        this.displayText = displayText;
        this.imageUri = imageUri;
        this.navigationState = navigationState;
    }

    public String getIndex() {
        return index;
    }

    public String getDisplayText() {
        return displayText;
    }

    public ArrayList<PersistentCommand> getCommands() {
        return commands;
    }

    public void addCommand(PersistentCommand command) {
        commands.add(command);
    }

    // Used to differentiate form vs module commands
    public NavIconState getNavigationState() {
        return navigationState;
    }

    public String getImageUri() {
        return imageUri;
    }

    @Override
    public String toString() {
        return "PersistentCommand [index=" + index + ", text=" + displayText + ", " +
                "navigationState=" + navigationState + ", imageUri=" + imageUri + "]";
    }

}
