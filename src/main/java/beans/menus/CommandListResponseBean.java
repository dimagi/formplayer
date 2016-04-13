package beans.menus;

import org.commcare.suite.model.MenuDisplayable;
import org.commcare.util.cli.MenuScreen;

/**
 * Created by willpride on 4/13/16.
 */
public class CommandListResponseBean extends MenuSessionBean{

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
            Command command = new Command();
            command.setIndex(i);
            command.setDisplayText(options[i].getDisplayText());
            command.setImageUri(options[i].getImageURI());
            command.setAudioUri(options[i].getAudioURI());
            commands[i] = command;
        }
        this.setCommands(commands);
    }
}
