package tests;

import application.Application;
import application.SessionController;
import auth.HqAuth;
import beans.NewSessionResponse;
import beans.QuestionBean;
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
    @Override
    public void setUp() throws IOException {
        super.setUp();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "test_restore.xml"));
    }

    @Test
    public void testNewForm() throws Exception {
        // setup files
        NewSessionResponse newSessionResponse = startNewSession("requests/new_form/new_form.json", "xforms/basic.xml");

        assert(newSessionResponse.getTitle().equals("Basic Form"));
        assert(newSessionResponse.getLangs().length == 2);

        // tree parsing
        QuestionBean[] tree = newSessionResponse.getTree();
        QuestionBean firstQuestion = tree[0];

        assert(firstQuestion.getCaption().equals("Enter a name:"));
        assert(tree.length == 1);
        assert(firstQuestion.getIx().contains("0,"));
        assert(firstQuestion.getDatatype().equals("str"));
    }

    @Test
    public void testNewForm2() throws Exception {
        NewSessionResponse newSessionResponse = startNewSession("requests/new_form/new_form_2.json", "xforms/question_types.xml");

        assert(newSessionResponse.getTitle().equals("Question Types"));
        assert(newSessionResponse.getLangs().length == 2);

        // tree parsing
        QuestionBean[] tree = newSessionResponse.getTree();
        assert(tree.length == 24);
        for(int i=0; i<tree.length; i++){
            QuestionBean currentBean = tree[i];
            switch(i){
                case 3:
                    assert currentBean.getBinding().equals("/data/q_numeric");
            }
        }
    }
}