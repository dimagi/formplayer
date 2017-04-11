package concurrency_tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import concurrency_tests.params.TestApp;
import concurrency_tests.params.TestDomain;
import utils.FileUtils;

import java.io.IOException;
import java.util.Vector;

public class ConcurrencyTester {

    private static Vector<String[]> testArgs;

    private static ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        loadTestArgs();
        startTests();
    }

    private static void loadTestArgs() throws IOException {

        String testParamsRaw = FileUtils.getFile(ConcurrencyTester.class.getClassLoader(), "concurrency/request_params.json");
        TestDomain[] testParams =  objectMapper.readValue(testParamsRaw, TestDomain[].class);

        testArgs = new Vector<>();
        for (TestDomain testDomain: testParams) {
            for (TestApp testApp: testDomain.getApps()) {
                for (String user: testDomain.getUsers()) {
                    for (String step: testApp.getSteps()) {
                        testArgs.add(new String[] {testApp.getId(), user, testDomain.getDomain(), step});
                    }
                }
            }
        }
    }

    private static void startTests() {
        for (String[] args: testArgs) {
            TestRequest request = new TestRequest(args[0], args[1], args[2], args[3]);
            SessionNavigatorThread requester = new SessionNavigatorThread(request);
            requester.start();
        }
    }

}
