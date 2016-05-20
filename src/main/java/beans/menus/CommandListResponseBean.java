package beans.menus;

import org.commcare.suite.model.MenuDisplayable;
import org.commcare.util.cli.MenuScreen;

import java.util.Arrays;

/**
 * Created by willpride on 4/13/16.
 */
public class CommandListResponseBean extends MenuBean {

    private Command[] commands;

    CommandListResponseBean(){}

    public Command[] getCommands() {
        return commands;
    }

    public void setCommands(Command[] commands) {
        this.commands = commands;
    }

    public CommandListResponseBean(MenuScreen menuScreen){
        this.setTitle(menuScreen.getScreenTitle());

        MenuDisplayable[] options = menuScreen.getMenuDisplayables();
        Command[] commands = new Command[options.length];
        for(int i = 0; i < options.length; i++){
            Command command = new Command(options[i], i);
            commands[i] = command;
        }
        this.setCommands(commands);
    }

    @Override
    public String toString(){
        return "CommandListResponseBean [commands=" + Arrays.toString(commands)
                + "MenuBean= " + super.toString() + "]";
    }
}
