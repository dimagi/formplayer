package qa;

/**
 * Created by willpride on 2/1/17.
 */
public class TestFailException extends Exception {
    public TestFailException(String cause) {
        super("Test failed with cause " + cause);
    }
}
