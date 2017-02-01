package beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.json.JSONArray;
import qa.QATestRunner;

/**
 * Created by willpride on 1/20/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunQAResponseBean {
    private boolean passed;
    private Exception cause;
    private String[] failures;

    // default constructor for Jackson
    public RunQAResponseBean(){}

    public RunQAResponseBean(QATestRunner qaTestRunner) {
        this.passed = qaTestRunner.didPass();
        this.cause = qaTestRunner.getCause();
        JSONArray failures = qaTestRunner.getCurrentState().getFailures();
        this.failures = new String[failures.length()];
        for (int i = 0; i < failures.length(); i++) {
            this.failures[i] = (String)failures.get(i);
        }
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public Exception getCause() {
        return cause;
    }

    public void setCause(Exception cause) {
        this.cause = cause;
    }

    public String[] getFailures() {
        return failures;
    }

    public void setFailures(String[] failures) {
        this.failures = failures;
    }
}
