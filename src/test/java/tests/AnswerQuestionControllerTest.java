package tests;

import application.SessionController;
import beans.AnswerQuestionRequestBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import objects.SerializableSession;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import repo.SessionRepo;
import services.XFormService;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class AnswerQuestionControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private SessionRepo sessionRepoMock;

    @Autowired
    private XFormService xFormServiceMock;

    @InjectMocks
    private SessionController sessionController;

    @Before
    public void setUp() throws IOException {
        Mockito.reset(sessionRepoMock);
        Mockito.reset(xFormServiceMock);
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(sessionController).build();
    }

    public JSONObject answerQuestionGetResult(String index, String answer, String sessionId) throws Exception {
        AnswerQuestionRequestBean answerQuestionBean = new AnswerQuestionRequestBean(index, answer, sessionId);
        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(answerQuestionBean);

        MvcResult answerResult = this.mockMvc.perform(
                post("/answer_question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andReturn();

        JSONObject object = new JSONObject(answerResult.getResponse().getContentAsString());
        return object;
    }

    @Test
    public void basicForm() throws Exception {

        SerializableSession serializableSession = new SerializableSession();
        serializableSession.setFormXml(FileUtils.getFile(this.getClass(), "xforms/basic.xml"));
        serializableSession.setInstanceXml(FileUtils.getFile(this.getClass(), "instances/basic_0.xml"));
        serializableSession.setId("test_id");

        when(sessionRepoMock.find(anyString()))
                .thenReturn(serializableSession);

        JSONObject object = answerQuestionGetResult("0","123","test_id");

        assert(object.has("tree"));
        String treeString = object.getString("tree");
        assert (object.get("status").equals("success"));
        System.out.println("answer response: " + treeString);
        JSONArray tree = new JSONArray(treeString);
        assert(tree.length() == 1);
        String jsonString = tree.getString(0);
        JSONObject jsonObject = new JSONObject(jsonString);
        assert(jsonObject.get("answer").equals("123"));

    }

    public class AnswerTree{
        private JSONObject object;
        private JSONArray tree;

        public AnswerTree(JSONObject object){
            this.object = object;
            assert(object.has("tree"));
            assert(object.get("status").equals("success"));
            String treeString = object.getString("tree");
            this.tree = new JSONArray(treeString);
        }

        public void assertTreeLength(int length){
            assert(tree.length() == length);
        }

        public void assertTreeAnswer(int index, String answer){
            String jsonString = tree.getString(index);
            JSONObject jsonObject = new JSONObject(jsonString);
            assert(jsonObject.get("answer").equals(answer));

        }
    }


    @Test
    public void questionTypesForm() throws Exception {

        SerializableSession serializableSession = new SerializableSession();
        serializableSession.setFormXml(FileUtils.getFile(this.getClass(), "xforms/question_types.xml"));
        serializableSession.setInstanceXml(FileUtils.getFile(this.getClass(), "instances/question_types_0.xml"));
        serializableSession.setId("test_id");

        when(sessionRepoMock.find(anyString()))
                .thenReturn(serializableSession);

        JSONObject object = answerQuestionGetResult("1","William Pride","test_id");

        AnswerTree answerTree = new AnswerTree(object);
        answerTree.assertTreeLength(24);
        answerTree.assertTreeAnswer(1, "William Pride");


        object = answerQuestionGetResult("2","123","test_id");
        answerTree = new AnswerTree(object);
        //answerTree.assertTreeAnswer(1, "William Pride");
        answerTree.assertTreeAnswer(2, "123");
        answerTree.assertTreeLength(24);
    }
}
