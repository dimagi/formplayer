package org.commcare.formplayer.beans.menus;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.checkerframework.checker.units.qual.A;
import org.commcare.formplayer.beans.menus.CommandUtils.NavIconState;

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
        String imageUri, NavIconState navigationState) {
        this.index = index;
        this.displayText = displayText;
        this.imageUri = imageUri;
        this.navigationState = navigationState;
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

    public String getImageUri() {
        return imageUri;
    }

    @Override
    public String toString() {
        return "PersistentCommand [index=" + index + ", text=" + displayText + ", " +
                "navigationState=" + navigationState + ", imageUri=" + imageUri + "]";
    }

}
