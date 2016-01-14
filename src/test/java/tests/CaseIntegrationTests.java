package tests;

import application.Application;
import hq.RestoreUtils;
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
import requests.FilterRequest;
import utils.FileUtils;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Created by willpride on 1/14/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Application.class)
@WebAppConfiguration
@WebIntegrationTest
public class CaseIntegrationTests {

    final String FILTER_URL = "http://localhost:8080/filter_cases";

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
    public void testFilterRequest() throws Exception {

        // setup files
        String filterRequestPayload = FileUtils.getFile(this.getClass(), "requests/filter/filter_cases.json");
        String restorePayload = FileUtils.getFile(this.getClass(), "test_restore.xml");

        //restore user ahead of time
        FilterRequest filterRequest = new FilterRequest(filterRequestPayload);
        RestoreUtils.restoreUser(filterRequest, restorePayload);

        JSONObject request = new JSONObject(filterRequestPayload);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(request.toString(), headers);

        ResponseEntity<String> restoreResponse = template.exchange(FILTER_URL, HttpMethod.POST, entity, String.class);

        String responseBody = restoreResponse.getBody();
        JSONObject jsonResponse = new JSONObject(responseBody);

        assert(jsonResponse.has("cases"));
        JSONArray caseArray = jsonResponse.getJSONArray("cases");
        assert(caseArray.length() == 3);
        assert(caseArray.getString(0).equals("2aa41fcf4d8a464b82b171a39959ccec"));
    }

    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);
    }

}
