package beans;

/**
 * Created by willpride on 8/18/16.
 */
public class PreviewRequestBean extends InstallRequestBean {
    private String commandId;

    public String getCommandId() {
        return commandId;
    }

    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }
}
