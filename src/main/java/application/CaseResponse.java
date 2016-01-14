package application;

import java.util.ArrayList;

/**
 * Created by willpride on 1/12/16.
 */
public class CaseResponse {
    String[] cases; // comma separated case list
    public CaseResponse(String caseString){
        String[] caseIds = caseString.split(",");
        ArrayList<String> caseIdArray = new ArrayList<String>();
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
}
