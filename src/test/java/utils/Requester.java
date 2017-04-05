package utils;

import java.util.Vector;

public class Requester {

    private static Vector<String[]> testArgs;

    private static final String[] testUsers = new String[] {"test", "t", "will", "derp"};
    private static final String[] normalUsers = new String[] {"derp", "skip", "t", "test"};

    public static void main(String[] args) throws Exception {
        loadTestArgs();
        startTests();
    }

    private static void loadTestArgs() {
        testArgs = new Vector<>();
        for (String user: testUsers) {
            testArgs.add(new String[] {"265ab21237afb735076e07efba8368a8", user, "test", "0"});
            testArgs.add(new String[] {"265ab21237afb735076e07efba8368a8", user, "test", "1"});
            testArgs.add(new String[] {"265ab21237afb735076e07efba8368a8", user, "test", "2"});
            testArgs.add(new String[] {"265ab21237afb735076e07efba8368a8", user, "test", "3"});
            testArgs.add(new String[] {"265ab21237afb735076e07efba8368a8", user, "test", "4"});
            testArgs.add(new String[] {"265ab21237afb735076e07efba8368a8", user, "test", "5"});
            testArgs.add(new String[] {"265ab21237afb735076e07efba8368a8", user, "test", "6"});

            testArgs.add(new String[] {"29a4790f25ebfe5abddedc3bd8c06e5c", user, "test", "0"});
            testArgs.add(new String[] {"29a4790f25ebfe5abddedc3bd8c06e5c", user, "test", "1"});

            testArgs.add(new String[] {"a277245b203f1e0a622d8ee1ba9e1ff0", user, "test", "0"});
            testArgs.add(new String[] {"a277245b203f1e0a622d8ee1ba9e1ff0", user, "test", "1"});
            testArgs.add(new String[] {"a277245b203f1e0a622d8ee1ba9e1ff0", user, "test", "2"});
            testArgs.add(new String[] {"a277245b203f1e0a622d8ee1ba9e1ff0", user, "test", "3"});
            testArgs.add(new String[] {"a277245b203f1e0a622d8ee1ba9e1ff0", user, "test", "4"});
        }

        for (String user: normalUsers) {
            //testArgs.add(new String[] {"c440986fe736ad8e7bb47f161565123f", user, "normal", "0"});
            testArgs.add(new String[] {"c440986fe736ad8e7bb47f161565123f", user, "normal", "1"});
            testArgs.add(new String[] {"2ea640e99c708a56120a7ac569ca5669", user, "normal", "0"});
            testArgs.add(new String[] {"2ea640e99c708a56120a7ac569ca5669", user, "normal", "1"});
        }

    }

    private static void startTests() {
        for (String[] args: testArgs) {
            TestRequest request = new TestRequest(args[0], args[1], args[2], args[3]);
            RequesterThread requester = new RequesterThread(request);
            requester.start();
        }

        for (String user: testUsers) {
            EndOfFormRequester requester = new EndOfFormRequester(new TestRequest("596efaf46b886069e7fdc307de01ae94", user, "test", ""));
            requester.start();
        }
    }

}
