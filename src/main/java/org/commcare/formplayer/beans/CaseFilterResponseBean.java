package org.commcare.formplayer.beans;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by willpride on 1/12/16.
 */
public class CaseFilterResponseBean {
    private String[] cases; // comma separated case list

    public CaseFilterResponseBean(){

    }
    public CaseFilterResponseBean(String caseString){
        String[] caseIds = caseString.split(",");
        ArrayList<String> caseIdArray = new ArrayList<>();
        for(String caseId: caseIds){
            if(!caseId.trim().equals("")){
                caseIdArray.add(caseId);
            }
        }
        cases = new String[caseIdArray.size()];
        for(int i=0; i< caseIdArray.size(); i++){
            cases[i] = caseIdArray.get(i);
        }
    }

    public String[] getCases() {
        return cases;
    }

    @Override
    public String toString(){
        return "CaseFitlerResponse: " + Arrays.toString(cases);
    }

}
