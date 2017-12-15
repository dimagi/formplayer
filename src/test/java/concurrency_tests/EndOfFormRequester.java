package concurrency_tests;

import beans.NewFormResponse;
import beans.SubmitRequestBean;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by willpride on 4/5/17.
 */
public class EndOfFormRequester extends SessionNavigatorThread {

    public EndOfFormRequester(TestRequest testRequest) {
        super(testRequest);
        testRequest.setSelections(new String[]{"2", "action 0"});
    }

    public void run() {
        Random randomGenerator = new Random();
        while (1 == 1) {
            try {
                String result = sessionNavigate(testRequest);
                Thread.sleep(randomGenerator.nextInt(10) * 1000);
                NewFormResponse response = new ObjectMapper().readValue(result, NewFormResponse.class);
                String sessionId = response.getSessionId();
                SubmitRequestBean submitRequestBean = new SubmitRequestBean();
                submitRequestBean.setSessionId(sessionId);
                HashMap<String, Object> answers = new HashMap<>();
                answers.put("0","123");
                answers.put("1", null);
                submitRequestBean.setAnswers(answers);
                submitRequestBean.setDomain(testRequest.getDomain());
                submitRequestBean.setPrevalidated(true);
                submitRequestBean.setUsername(testRequest.getUsername());
                result = makeRequest(submitRequestBean, "submit-all");
                testRequest.loadNewResult(result);
                System.out.println(String.format("TestRequest %s passed.", testRequest));
                Thread.sleep(randomGenerator.nextInt(10) * 1000);
            } catch (InterruptedException | IOException e) {
                System.out.println(e);
            } catch (RuntimeException e) {
                System.err.println(String.format("TestRequest %s failed.", testRequest));
                throw e;
            }
        }
    }
}
