package beans;

import java.util.Arrays;

/**
 * Created by willpride on 4/25/16.
 */
public class MenuSelectRepeater extends SessionBean{
    public MenuSelectRepeater(){}

    private String[] selections;

    public String[] getSelections() {
        return selections;
    }

    public void setSelections(String[] selections) {
        this.selections = selections;
    }

    @Override
    public String toString(){
        return "MenuSelectRepeater [sessionId=" + sessionId + ", selections="
                + Arrays.toString(selections) + "]";
    }
}
