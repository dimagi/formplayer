package org.commcare.formplayer.beans;

/**
 * Created by willpride on 1/12/16.
 */
public class CaseFilterFullResponseBean {
    private CaseBean[] cases; // comma separated case list

    public CaseFilterFullResponseBean(){

    }
    public CaseFilterFullResponseBean(CaseBean[] caseBeans) {
        cases = caseBeans;
    }

    public CaseBean[] getCases() {
        return cases;
    }
}
