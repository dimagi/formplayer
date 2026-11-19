package org.commcare.formplayer.tests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.commcare.formplayer.beans.EvaluateXPathMenuRequestBean;
import org.commcare.formplayer.beans.EvaluateXPathRequestBean;
import org.commcare.formplayer.beans.EvaluateXPathResponseBean;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.SessionNavigationBean;
import org.commcare.formplayer.beans.SessionRequestBean;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

@WebMvcTest
public class MenuDebuggerTests extends BaseTestClass {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("loaddomain", "loaduser");
    }

    @Test
    public void testMenuDebugger() throws Exception {
        // Menu session should be saved so let's run some menu xpath queries against it
        EvaluateXPathResponseBean evaluateXpathResponseBean = evaluateMenuXpath(
                "requests/evaluate_xpath/evaluate_xpath_menu.json"
        );
        Assertions.assertEquals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE,
                evaluateXpathResponseBean.getStatus());
        // Hack to not have to parse the XML returned
        Assertions.assertTrue(evaluateXpathResponseBean.getOutput().contains("15"));
    }

    @Test
    public void testFormattedQuestionsGrantedWithEditData() throws Exception {
        NewFormResponse newFormResponse =
                sessionNavigate(new String[]{"0", "0"}, "formnav", NewFormResponse.class);
        when(webClientMock.post(anyString(), any()))
                .thenReturn("{\"form_data\":\"<div/>\",\"form_questions\":[]}");

        SessionRequestBean bean = new SessionRequestBean();
        bean.setSessionId(newFormResponse.getSessionId());
        postDebuggerRequest(Constants.URL_DEBUGGER_FORMATTED_QUESTIONS, bean)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formattedQuestions").value("<div/>"));
    }

    @Test
    public void testMenuDebuggerContentGrantedWithEditData() throws Exception {
        postDebuggerRequestWithInstallReference("requests/evaluate_xpath/evaluate_xpath_menu.json",
                Constants.URL_DEBUGGER_MENU_CONTENT, SessionNavigationBean.class)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId").value("loadappid"));
    }

    @Test
    public void testFormattedQuestionsDeniedWithoutEditData() throws Exception {
        setEditDataPermission(false);
        SessionRequestBean bean = new SessionRequestBean();
        bean.setSessionId("sessionid");
        postDebuggerRequest(Constants.URL_DEBUGGER_FORMATTED_QUESTIONS, bean)
                .andExpect(status().isForbidden());
    }

    @Test
    public void testMenuDebuggerContentDeniedWithoutEditData() throws Exception {
        setEditDataPermission(false);
        SessionNavigationBean bean = new SessionNavigationBean();
        postDebuggerRequest(Constants.URL_DEBUGGER_MENU_CONTENT, bean)
                .andExpect(status().isForbidden());
    }

    @Test
    public void testMenuEvaluateXpathDeniedWithoutEditData() throws Exception {
        setEditDataPermission(false);
        EvaluateXPathMenuRequestBean bean = new EvaluateXPathMenuRequestBean();
        bean.setXpath("true()");
        postDebuggerRequest(Constants.URL_EVALUATE_MENU_XPATH, bean)
                .andExpect(status().isForbidden());
    }

    @Test
    public void testEvaluateXpathDeniedWithoutEditData() throws Exception {
        setEditDataPermission(false);
        EvaluateXPathRequestBean bean = new EvaluateXPathRequestBean();
        bean.setSessionId("sessionid");
        bean.setXpath("true()");
        postDebuggerRequest(Constants.URL_EVALUATE_XPATH, bean)
                .andExpect(status().isForbidden());
    }
}
