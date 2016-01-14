package tests;

import application.Application;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.UserSqlSandbox;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import utils.FileUtils;

import java.util.Arrays;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Created by willpride on 1/14/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Application.class)
@WebAppConfiguration
@WebIntegrationTest
public class NewFormTests {

    final String NEW_FORM_URL = "http://localhost:8080/new_form";

    RestTemplate template = new TestRestTemplate();

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void setup() throws Exception {
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void testNewForm() throws Exception {
        // setup files
        String requestPayload = FileUtils.getFile(this.getClass(), "requests/new_form/new_form.json");

        JSONObject request = new JSONObject(requestPayload);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(request.toString(), headers);

        ResponseEntity<String> restoreResponse = template.exchange(NEW_FORM_URL, HttpMethod.POST, entity, String.class);

        String responseBody = restoreResponse.getBody();
        JSONObject jsonResponse = new JSONObject(responseBody);

        assert(jsonResponse.has("tree"));
        assert(jsonResponse.has("langs"));
        assert(jsonResponse.has("title"));

        assert(jsonResponse.getString("title").equals("Basic Form"));
        assert(jsonResponse.getJSONArray("langs").length() == 2);

        // tree parsing
        String tree = jsonResponse.getString("tree");
        JSONArray treeArray = new JSONArray(tree);
        JSONObject treeObject = new JSONObject(treeArray.get(0).toString());

        assert(treeObject.getString("caption").equals("Enter a name:"));
        assert(14 == treeObject.length());
        assert(treeObject.getString("ix").contains("0,"));
        assert(treeObject.getString("datatype").equals("str"));
    }

    @Test
    public void testNewForm2() throws Exception {
        // setup files
        String requestPayload = FileUtils.getFile(this.getClass(), "requests/new_form/new_form_2.json");

        JSONObject request = new JSONObject(requestPayload);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(request.toString(), headers);

        ResponseEntity<String> restoreResponse = template.exchange(NEW_FORM_URL, HttpMethod.POST, entity, String.class);

        String responseBody = restoreResponse.getBody();
        JSONObject jsonResponse = new JSONObject(responseBody);

        assert(jsonResponse.has("tree"));
        assert(jsonResponse.has("langs"));
        assert(jsonResponse.has("title"));

        assert(jsonResponse.getString("title").equals("Question Types"));
        assert(jsonResponse.getJSONArray("langs").length() == 2);

        // tree parsing
        String tree = jsonResponse.getString("tree");
        JSONArray treeArray = new JSONArray(tree);
        assert(treeArray.length() == 24);
        for(int i=0; i<treeArray.length(); i++){
            String currentString = treeArray.get(i).toString();
            JSONObject currentQuestion = new JSONObject(currentString);
            switch(i){
                case 3:
                    assert currentQuestion.get("binding").equals("/data/q_numeric");
            }
        }
    }

    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);
    }

}
