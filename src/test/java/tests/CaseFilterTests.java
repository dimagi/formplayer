package tests;

import application.CaseController;
import application.SessionController;
import auth.HqAuth;
import com.fasterxml.jackson.databind.ObjectMapper;
import hq.CaseAPIs;
import hq.RestoreUtils;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import repo.SessionRepo;
import requests.FilterRequest;
import org.apache.commons.io.IOUtils;
import services.RestoreService;
import services.XFormService;
import utils.FileUtils;
import utils.TestContext;

import java.io.File;
import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class CaseFilterTests {

    private MockMvc mockMvc;

    @Autowired
    private RestoreService restoreServiceMock;

    @InjectMocks
    private CaseController caseController;

    @Before
    public void setUp() {
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);
        Mockito.reset(restoreServiceMock);
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(caseController).build();
        when(restoreServiceMock.getRestoreXml(anyString(), anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "test_restore.xml"));
    }

    @Test
    public void testRestoreFilter() throws Exception {
        String filterRequestPayload = FileUtils.getFile(this.getClass(), "requests/filter/filter_cases.json");

        MvcResult result = this.mockMvc.perform(
                post("/filter_cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterRequestPayload))
                .andExpect(status().isOk())
                .andReturn();

        JSONObject responseObject =  new JSONObject(result.getResponse().getContentAsString());
        JSONArray caseArray = responseObject.getJSONArray("cases");
        assert(caseArray.length() == 3);
        assert(caseArray.get(0).equals("2aa41fcf4d8a464b82b171a39959ccec"));

        filterRequestPayload = FileUtils.getFile(this.getClass(), "requests/filter/filter_cases_2.json");
        result = this.mockMvc.perform(
                post("/filter_cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterRequestPayload))
                .andExpect(status().isOk())
                .andReturn();

        responseObject =  new JSONObject(result.getResponse().getContentAsString());
        caseArray = responseObject.getJSONArray("cases");
        assert(caseArray.length() == 9);
        assert(caseArray.get(8).equals("35cb37fbff054703b249df0892c09e15"));

        filterRequestPayload = FileUtils.getFile(this.getClass(), "requests/filter/filter_cases_3.json");
        result = this.mockMvc.perform(
                post("/filter_cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterRequestPayload))
                .andExpect(status().isOk())
                .andReturn();

        responseObject =  new JSONObject(result.getResponse().getContentAsString());
        caseArray = responseObject.getJSONArray("cases");
        assert(caseArray.length() == 1);
        assert(caseArray.get(0).equals("e7ed3658d7394415a4bba5edc7055f1d"));
    }

    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);
    }
}