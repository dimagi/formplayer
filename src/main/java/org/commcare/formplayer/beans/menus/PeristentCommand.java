package org.commcare.formplayer.beans.menus;

import org.checkerframework.checker.units.qual.A;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.suite.model.MenuDisplayable;

import java.util.ArrayList;

/**
 * Model used to represent a persistent menu in FP HTTP API
 */
public class PeristentCommand {

    private String index;
    private String displayText;
    private ArrayList<PeristentCommand> commands = new ArrayList<>();

    /**
     * serialization only
     */
    public PeristentCommand() {
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
}
