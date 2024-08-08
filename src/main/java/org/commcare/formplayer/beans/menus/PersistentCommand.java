package org.commcare.formplayer.beans.menus;

import java.util.ArrayList;

import lombok.EqualsAndHashCode;

/**
 * Model used to represent a persistent menu in FP HTTP API
 */
@EqualsAndHashCode
public class PersistentCommand {

    private String index;
    private String displayText;
    private ArrayList<PersistentCommand> commands = new ArrayList<>();

    /**
     * serialization only
     */
    public PersistentCommand() {
    }

    public PersistentCommand(String index, String displayText) {
        this.index = index;
        this.displayText = displayText;
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
}
