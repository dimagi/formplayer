package beans;

import java.util.Arrays;

/**
 * Created by willpride on 4/28/16.
 */
public class SessionNavigationBean extends InstallRequestBean {
    private String[] selections;

    public String[] getSelections() {
        return selections;
    }

    public void setSelections(String[] selections) {
        this.selections = selections;
    }

    @Override
    public String toString() {
        return "SessionNavigationBean [selections="
                + Arrays.toString(selections) + "]";
    }
}
