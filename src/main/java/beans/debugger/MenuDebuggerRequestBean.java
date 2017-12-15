package beans.debugger;

import beans.SessionNavigationBean;

/**
 * Created by benrudolph on 6/15/17.
 */
public class MenuDebuggerRequestBean extends SessionNavigationBean {

    private String[] steps;

    public String[] getSteps() {
        return steps;
    }

    public void setSteps(String[] steps) {
        this.steps = steps;
    }
}
