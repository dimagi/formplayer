package tests;

import application.Application;
import application.SessionController;
import auth.HqAuth;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.UserSqlSandbox;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import repo.SessionRepo;
import services.RestoreService;
import services.XFormService;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;
import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Created by willpride on 1/14/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class NewFormTests extends BaseTestClass{

    @Before
    public void setUp() throws IOException {
        super.setUp();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "test_restore.xml"));
    }

    @Test
    public void testNewForm() throws Exception {
        // setup files

        when(xFormServiceMock.getFormXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "xforms/basic.xml"));

        String requestPayload = FileUtils.getFile(this.getClass(), "requests/new_form/new_form.json");

        JSONObject request = new JSONObject(requestPayload);

        MvcResult result = this.mockMvc.perform(
                post("/new_session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JSONObject jsonResponse = new JSONObject(responseBody);

        assert(jsonResponse.has("tree"));
        assert(jsonResponse.has("langs"));
        assert(jsonResponse.has("title"));
        assert(jsonResponse.has("session_id"));

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
        when(xFormServiceMock.getFormXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "xforms/question_types.xml"));
        String requestPayload = FileUtils.getFile(this.getClass(), "requests/new_form/new_form_2.json");

        JSONObject request = new JSONObject(requestPayload);

        MvcResult result = this.mockMvc.perform(
                post("/new_session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JSONObject jsonResponse = new JSONObject(responseBody);

        assert(jsonResponse.has("tree"));
        assert(jsonResponse.has("langs"));
        assert(jsonResponse.has("title"));
        assert(jsonResponse.has("session_id"));

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