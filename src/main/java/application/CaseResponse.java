package application;

/**
 * Created by willpride on 1/12/16.
 */
public class CaseResponse {
    String cases; // comma separated case list
    public CaseResponse(String cases){
        this.cases = cases;
    }

    public String getCases() {
        return cases;
    }
}
