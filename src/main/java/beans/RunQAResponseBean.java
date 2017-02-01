package beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import qa.QATestRunner;

/**
 * Created by willpride on 1/20/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunQAResponseBean {
    private boolean passed;
    private Exception cause;

    // default constructor for Jackson
    public RunQAResponseBean(){}

    public RunQAResponseBean(QATestRunner qaTestRunner) {
        this.passed = qaTestRunner.didPass();
        this.cause = qaTestRunner.getCause();
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
}
