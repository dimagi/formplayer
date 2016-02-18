package beans;

/**
 * Created by willpride on 2/5/16.
 */
public class MenuSelectBean extends SessionBean{
    private String selection;

    public String getSelection() {
        return selection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
    }

    @Override
    public String toString(){
        return "MenuSelectBean [sessionId=" + sessionId + ", selection=" + selection + "]";
    }
}
