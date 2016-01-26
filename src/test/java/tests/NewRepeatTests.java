package tests;

import application.Application;
import application.SessionController;
import auth.HqAuth;
import beans.NewRepeatRequestBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.UserSqlSandbox;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import repo.SessionRepo;
import services.XFormService;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by willpride on 1/14/16.
 */
@SpringApplicationConfiguration(classes = Application.class)   // 2
@WebAppConfiguration   // 3
@IntegrationTest()
@RunWith(MockitoJUnitRunner.class)
public class NewRepeatTests {

    final String NEW_FORM_URL = "/new_session";

    RestTemplate template = new TestRestTemplate();

    private MockMvc mockMvc;

    @Mock
    private XFormService xFormServiceMock;

    @Before
    public void setUp() throws IOException {
        WebApplicationContext context = (WebApplicationContext) SpringApplication.run(Application.class);
        mockMvc = MockMvcBuilders.<StandaloneMockMvcBuilder>webAppContextSetup(context).build();
    }

    @Test
    public void testRepeat() throws Exception {

        when(xFormServiceMock.getFormXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "xforms/repeat.xml"));

        String requestPayload = FileUtils.getFile(this.getClass(), "requests/new_form/new_form.json");

        JSONObject request = new JSONObject(requestPayload);

        MvcResult result = this.mockMvc.perform(
                post("/new_session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString())).andReturn();

        String responseBody = result.getResponse().getContentAsString();

        System.out.println("jResponse body: " + responseBody);

        JSONObject jsonResponse = new JSONObject(responseBody);

        System.out.println("json response: " + jsonResponse);

        String sessionId = jsonResponse.getString("session_id");

        System.out.println("session id: " + sessionId);

        String repeatRequestPayload = FileUtils.getFile(this.getClass(), "requests/new_repeat/new_repeat.json");

        ObjectMapper mapper = new ObjectMapper();
        NewRepeatRequestBean newRepeatRequestBean = mapper.readValue(repeatRequestPayload, NewRepeatRequestBean.class);
        newRepeatRequestBean.setSessionId(sessionId);

        String requestString = mapper.writeValueAsString(newRepeatRequestBean);

        ResultActions repeatResult = mockMvc.perform(get("/new_repeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestString));

        String repeatResultString = repeatResult.andReturn().getResponse().getContentAsString();

        System.out.println("Repeat Result: " + repeatResultString);

        JSONObject resultJson = new JSONObject(repeatResultString);

        System.out.println("Repeat Result: " + resultJson);

    }

    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);
    }

}