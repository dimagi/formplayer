package qa;

import org.json.JSONArray;

/**
 * Created by willpride on 2/1/17.
 */
public class TestState {
    private JSONArray steps;
    private JSONArray failures;
    private boolean passed;

    public TestState() {
        this.steps = new JSONArray();
        this.failures = new JSONArray();
        passed = true;
    }

    public JSONArray getSteps() {
        return steps;
    }

    public void setSteps(JSONArray steps) {
        this.steps = steps;
    }

    public void addStep(String step) {
        steps.put(step);
    }

    public void addFailure(String s) {
        passed = false;
        failures.put(s);
    }

    public JSONArray getFailures() {
        return failures;
    }

    public void setFailures(JSONArray failures) {
        this.failures = failures;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }
}
