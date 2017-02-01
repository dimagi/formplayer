package qa;

import beans.AnswerQuestionRequestBean;
import beans.FormEntryResponseBean;
import beans.SessionRequestBean;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    boolean passed;
    ObjectMapper objectMapper;

    FormEntryResponseBean formEntry;

    String appId;
    String username;
    String domain;
    String password;

    public QATestRunner(String testPlan) throws Exception {
        this.testPlan = testPlan;
        this.errors = new ArrayList<>();
        this.passed = true;
        this.objectMapper = new ObjectMapper();
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
                passed = false;
            }
        }
    }

    private void processPickle(Pickle pickle) throws Exception {
        for (PickleStep step : pickle.getSteps()) {
            processStep(step);
        }
    }

    private void processStep(PickleStep step) throws Exception {
        String text = step.getText();
        checkInstall(text);
        checkModule(text);
        checkNext(text);
        checkAnswer(text);
    }

    private void checkAnswer(String text) throws Exception {
        String pattern = "^I enter text (.*)$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(text);
        if (m.find()) {
            String[] args = text.split("\"");
            doAnswer(args[1]);
        }
    }

    private void checkNext(String text) throws Exception {
        String pattern = "^Next$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(text);
        if (m.find()) {
            doNext();
        }
    }

    private void checkInstall(String text) throws Exception {
        String pattern = "^I install the app with id (.*)$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(text);
        if (m.find()) {
            String[] args = text.split("\"");
            doInstall(args[1], args[3], args[5], args[7]);
        }
    }

    private void checkModule(String text) throws Exception {
        String pattern = "^I select (form|module) (.*)$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(text);
        if (m.find()) {
            String[] args = text.split("\"");
            selectModule(args[1]);
        }
    }

    private JSONArray getSelections() throws JSONException {
        if (lastResponseJson.has("selections")) {
            return lastResponseJson.getJSONArray("selections");
        } else {
            return new JSONArray();
        }
    }

    private void doNext() throws Exception {
        SessionRequestBean request = new SessionRequestBean();
        request.setSessionId(formEntry.getSessionId());
        JSONObject nextJson = new JSONObject(objectMapper.writeValueAsString(request));
        makePostRequest("next_index", nextJson);
    }

    private void doInstall(String appId, String domain, String username, String password) throws Exception {
        JSONObject json = new JSONObject();
        this.appId = appId;
        this.domain = domain;
        this.username = username;
        this.password = password;
        makePostRequest("navigate_menu", json);
    }

    private void doAnswer(String answer) throws Exception {
        AnswerQuestionRequestBean request = new AnswerQuestionRequestBean();
        request.setAnswer(answer);
        request.setSessionId(formEntry.getSessionId());
        JSONObject json = new JSONObject(objectMapper.writeValueAsString(request));
        makePostRequest("answer", json);
    }

    private void selectModule(String arg) throws Exception {
        if(!lastResponseJson.has("commands")) {
            return;
        }
        JSONObject requestBody = new JSONObject();
        JSONArray commands = (JSONArray) lastResponseJson.get("commands");
        boolean matched = false;
        for (int i = 0; i < commands.length(); i++) {
            if((commands.get(i).toString()).contains(arg)) {
                JSONArray selections = getSelections();
                selections.put(i);
                requestBody.put("selections", selections);
                matched = true;
                break;
            }
        }
        if (!matched) {
            throw new RuntimeException("Argument "  + arg + " didn't match commands " + commands);
        }
        makePostRequest("navigate_menu", requestBody);
    }

    private void makePostRequest(String url, JSONObject body) throws Exception {
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost("http://localhost:8090/" + url + "/");

        body.put("app_id", appId);
        body.put("domain", domain);
        body.put("username", username);
        body.put("password", password);
        body.put("oneQuestionPerScreen", true);

        HttpEntity e = new StringEntity(body.toString());
        httppost.setEntity(e);
        httppost.setHeader("Cookie", "sessionid="+ "ygvolsnvniy72p4wmlgipb5xlg0see8q");
        httppost.setHeader("Accept", "application/json");
        httppost.setHeader("Content-type", "application/json");

        //Execute and get the response.
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            InputStream instream = entity.getContent();
            try {
                String responseString = getStringFromInputStream(instream);
                handleResponse(responseString);
            } finally {
                instream.close();
            }
        }
    }

    private void handleResponse(String lastResponse) {
        lastResponseJson = new JSONObject(lastResponse);
        try {
            formEntry = new ObjectMapper().readValue(lastResponse, FormEntryResponseBean.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // convert InputStream to String
    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();
    }

    public boolean didPass() {
        return passed;
    }

    public Exception getCause() {
        return cause;
    }
}
