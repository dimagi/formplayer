package beans.debugger;

/**
 * Created by willpride on 11/16/16.
 */
public class FunctionAutocompletable extends QuestionResponseItem {
    public FunctionAutocompletable(String functionName) {
        super(functionName + "()", functionName, "Function");
    }
}
