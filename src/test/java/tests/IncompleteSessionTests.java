package tests;

import application.Application;
import application.FormController;
import auth.HqAuth;
import beans.GetSessionsBean;
import beans.GetSessionsResponse;
import beans.NewFormSessionResponse;
import beans.QuestionBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.javarosa.core.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import util.Constants;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Created by willpride on 1/14/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(Application.class)
@WebIntegrationTest
public class IncompleteSessionTests {

    private ObjectMapper mapper;
    private RestTemplate restTemplate = new TestRestTemplate();

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    public void testGetSessions() throws Exception {
        HashMap<String, Object> requestBody = new HashMap<String, Object>();
        requestBody.put("username", "testuser");
        requestBody.put("domain", "testdomain");
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);

        //Creating http entity object with request body and headers
        HttpEntity<String> httpEntity =
                new HttpEntity<String>(mapper.writeValueAsString(requestBody), requestHeaders);

        //Invoking the API
        GetSessionsResponse apiResponse =
                    restTemplate.postForObject("http://localhost:8090/" + Constants.URL_GET_SESSIONS,
                        httpEntity,
                        GetSessionsResponse.class,
                        Collections.EMPTY_MAP);
        System.out.println("Api Response: " + apiResponse);

    }

    private String urlPrepend(String string){
        return "/" + string;
    }

}