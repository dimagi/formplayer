package qa;

import beans.NewFormResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import gherkin.AstBuilder;
import gherkin.Parser;
import gherkin.ast.GherkinDocument;
import gherkin.pickles.Compiler;
import gherkin.pickles.Pickle;
import gherkin.pickles.PickleStep;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import qa.steps.*;
import util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by willpride on 2/1/17.
 */
public class QATestRunner {

    String testPlan;
    JSONObject lastResponseJson;
    ArrayList<String> errors;
    Exception cause;
    ObjectMapper objectMapper;

    NewFormResponse formEntry;

    String appId;
    String username;
    String domain;
    String password;

    StepDefinition[] stepDefinitions;
    TestState currentState;

    public QATestRunner(String testPlan, String appId, String domain, String username, String password) throws Exception {
        this.testPlan = testPlan;
        this.currentState = new TestState();
        this.appId = appId;
        this.domain = domain;
        this.username = username;
        this.password = password;
        this.errors = new ArrayList<>();
        this.objectMapper = new ObjectMapper();
        stepDefinitions = new StepDefinition[5];
        stepDefinitions[0] = new AnswerStep();
        stepDefinitions[1] = new NextStep();
        stepDefinitions[2] = new MenuStep();
        stepDefinitions[3] = new InstallStep();
        stepDefinitions[4] = new SeeStep();
        runTests();
    }

    public void runTests() {
        Parser<GherkinDocument> parser = new Parser<>(new AstBuilder());
        GherkinDocument gherkinDocument = parser.parse(testPlan);
        List<Pickle> pickles = new Compiler().compile(gherkinDocument);
        for (Pickle pickle : pickles) {
            try {
                processPickle(pickle);
            } catch (Exception e) {
                this.cause = e;
                e.printStackTrace();
                currentState.addFailure(e.getMessage());
            }
        }
    }

    private boolean matchRegex(String text, String regex) {
        Pattern r = Pattern.compile(regex);
        Matcher m = r.matcher(text);
        return m.find();
    }

    private String[] getArgs(String text, String regex) {
        Pattern r = Pattern.compile(regex);
        Matcher matcher = r.matcher(text);
        ArrayList<String> argList = new ArrayList<>();
        if (matcher.find() && matcher.groupCount() > 0) {
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    try {
                        argList.add(matcher.group(i));
                    } catch(IllegalStateException e) {
                        //
                    }
                }
            String[] args = new String[matcher.groupCount()];
            argList.toArray(args);
            return args;
        } else {
            return null;
        }
    }

    private void processPickle(Pickle pickle) throws Exception {
        for (PickleStep step : pickle.getSteps()) {
            processStep(step);
        }
    }

    private void processStep(PickleStep step) throws Exception {
        String text = step.getText();
        System.out.println("Running step " + text);
        for (StepDefinition stepDefinition: stepDefinitions) {
            if (matchRegex(text, stepDefinition.getRegularExpression())) {
                String[] args = getArgs(text, stepDefinition.getRegularExpression());
                try {
                    stepDefinition.doWork(lastResponseJson, currentState, args);
                } catch(TestFailException e) {
                    currentState.addFailure(text + " failed with cause " + e);
                }
                try {
                    JSONObject requestBody = stepDefinition.getPostBody(lastResponseJson, currentState, args);
                    if (requestBody != null) {
                        makePostRequest(stepDefinition.getUrl(), requestBody);
                    }
                } catch(TestFailException e) {
                    currentState.addFailure(text + " failed with cause " + e);
                }
            }
        }
    }

    private void makePostRequest(String url, JSONObject body) throws Exception {
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost("http://localhost:8090/" + url + "/");

        body.put("app_id", appId);
        body.put("domain", domain);
        body.put("username", username);
        body.put("password", password);

        if (formEntry != null) {
            body.put("session_id", formEntry.getSessionId());
        }

        body.put("oneQuestionPerScreen", true);

        HttpEntity e = new StringEntity(body.toString());
        httppost.setEntity(e);
        httppost.setHeader("Cookie", "sessionid="+ "exrjtza7k4q8xpq9xud9ogkg4x96i8df");
        httppost.setHeader("Accept", "application/json");
        httppost.setHeader("Content-type", "application/json");

        //Execute and get the response.
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            InputStream instream = entity.getContent();
            try {
                String responseString = StringUtils.getStringFromInputStream(instream);
                handleResponse(responseString);
            } finally {
                instream.close();
            }
        }
    }

    private void handleResponse(String lastResponse) {
        lastResponseJson = new JSONObject(lastResponse);
        if (formEntry == null) {
            try {
                formEntry = new ObjectMapper().readValue(lastResponse, NewFormResponse.class);
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
    }

    public boolean didPass() {
        return currentState.isPassed();
    }

    public Exception getCause() {
        return cause;
    }

    public TestState getCurrentState() {
        return currentState;
    }
}
