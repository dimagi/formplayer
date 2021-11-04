package org.commcare.formplayer.tests;


import org.commcare.formplayer.beans.EvaluateXPathResponseBean;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.beans.menus.QueryResponseBean;
import org.commcare.formplayer.objects.QueryData;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.session.FormSession;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ContextConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class FormEntryWithQueryTests extends BaseTestClass{

    @Autowired
    CacheManager cacheManager;

    @Captor
    ArgumentCaptor<URI> uriCaptor;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("caseclaimdomain", "caseclaimusername");
        cacheManager.getCache("case_search").clear();
        configureQueryMock();
    }

    @Test
    public void testFormEntryWithQuery() throws Exception {
        // selecting the command containing form with `query` should launch the query screen
        sessionNavigateWithQuery(new String[]{"1"},
                "caseclaimquery",
                null,
                false,
                QueryResponseBean.class);


        Hashtable<String, String> inputs = new Hashtable<>();
        QueryData queryData = new QueryData();
        queryData.setInputs("m1", inputs);
        queryData.setExecute("m1", true);

        sessionNavigateWithQuery(new String[]{"1"},
                "caseclaimquery",
                queryData,
                false,
                EntityListResponse.class);

        // Check if form's query was executed
        verify(webClientMock, times(1)).get(uriCaptor.capture());
        List<URI> uris = uriCaptor.getAllValues();
        // when default search, prompts doesn't get included
        assert uris.get(0).equals(new URI("http://localhost:8000/a/test-1/phone/search/dec220eae9974c788654f23320f3a8d3/?commcare_registry=shubham&case_type=case"));


        // Open the form with `query` blocks
        NewFormResponse formResponse = sessionNavigateWithQuery(new String[]{"1", "0156fa3e-093e-4136-b95c-01b13dae66c6", "0"},
                "caseclaimquery",
                queryData,
                false,
                NewFormResponse.class);

        // verify the second query block to fetch the remote case was executed
        verify(webClientMock, times(2)).get(uriCaptor.capture());
        uris = uriCaptor.getAllValues();
        // when default search, prompts doesn't get included
        assert uris.get(2).equals(new URI("http://localhost:8000/a/test-1/phone/registry_case/dec220eae9974c788654f23320f3a8d3/?commcare_registry=shubham&case_type=case&case_id=0156fa3e-093e-4136-b95c-01b13dae66c6"));

        // see if the instance is retained into the form session
        checkXpath(
                formResponse,
                "instance('registry')/results/case[@case_type='case']/case_name",
                "Burt Maclin"
        );

        // make sure that the instance in the form session is using the case template
        SerializableFormSession serializableFormSession = formSessionService.getSessionById(formResponse.getSessionId());
        FormSession formSession = getFormSession(serializableFormSession);
        EvaluationContext evaluationContext = formSession.getFormEntryModel().getForm().getEvaluationContext();
        ExternalDataInstance registry = (ExternalDataInstance) evaluationContext.getInstance("registry");
        assertTrue(registry.useCaseTemplate());
    }

    @Test
    public void testNavigationToFormEntryWithMultipleQueries() throws Exception {
        // select module 2
        sessionNavigateWithQuery(new String[]{"2"},
                "caseclaimquery",
                null,
                false,
                QueryResponseBean.class);


        Hashtable<String, String> inputs = new Hashtable<>();
        QueryData queryData = new QueryData();
        queryData.setInputs("m2", inputs);
        queryData.setExecute("m2", true);

        // execute search query
        sessionNavigateWithQuery(new String[]{"2"},
                "caseclaimquery",
                queryData,
                false,
                EntityListResponse.class);

        // Check if form's query was executed
        verify(webClientMock, times(1)).get(uriCaptor.capture());
        List<URI> uris = uriCaptor.getAllValues();
        // when default search, prompts doesn't get included
        assert uris.get(0).equals(new URI("http://localhost:8000/a/test-1/phone/search/dec220eae9974c788654f23320f3a8d3/?commcare_registry=shubham&case_type=case"));

        // Select a case
        CommandListResponseBean menuResponse = sessionNavigateWithQuery(new String[]{"2", "0156fa3e-093e-4136-b95c-01b13dae66c6"},
                "caseclaimquery",
                queryData,
                false,
                CommandListResponseBean.class);

        assertEquals(1, menuResponse.getCommands().length);
        assertEquals("Dedupe Form", menuResponse.getCommands()[0].getDisplayText());

        // verify the second query block to fetch the remote case was executed as well as the 3rd query block
        // to do a custom lookup
        verify(webClientMock, times(3)).get(uriCaptor.capture());
        uris = uriCaptor.getAllValues();
        // when default search, prompts doesn't get included
        assert uris.get(2).equals(new URI("http://localhost:8000/a/test-1/phone/registry_case/dec220eae9974c788654f23320f3a8d3/?commcare_registry=shubham&case_type=case&case_id=0156fa3e-093e-4136-b95c-01b13dae66c6"));
        assert uris.get(3).equals(new URI("http://localhost:8000/a/test-1/phone/registry_case/dec220eae9974c788654f23320f3a8d3/?commcare_registry=shubham&case_type=case&case_id=dupe_case_id"));

        // Open the form
        NewFormResponse formResponse = sessionNavigateWithQuery(new String[]{"2", "0156fa3e-093e-4136-b95c-01b13dae66c6", "0"},
                "caseclaimquery",
                queryData,
                false,
                NewFormResponse.class);

        verify(webClientMock, times(3)).get(uriCaptor.capture());
        uris = uriCaptor.getAllValues();
        assert uris.get(2).equals(new URI("http://localhost:8000/a/test-1/phone/registry_case/dec220eae9974c788654f23320f3a8d3/?commcare_registry=shubham&case_type=case&case_id=0156fa3e-093e-4136-b95c-01b13dae66c6"));
        assert uris.get(3).equals(new URI("http://localhost:8000/a/test-1/phone/registry_case/dec220eae9974c788654f23320f3a8d3/?commcare_registry=shubham&case_type=case&case_id=dupe_case_id"));

        // check we can access the 'registry' instance in the form
        checkXpath(
                formResponse,
                "instance('registry')/results/case[@case_type='case']/case_name",
                "Burt Maclin"
        );

        // check we can access the 'duplicate' instance in the form
        checkXpath(
                formResponse,
                "instance('duplicate')/results/case[@case_id='dupe_case_id']/case_name",
                "Duplicate of Burt"
        );
    }

    private void checkXpath(NewFormResponse formResponse, String xpath, String expectedValue) throws Exception {
        EvaluateXPathResponseBean evaluateXPathResponseBean = evaluateXPath(formResponse.getSessionId(), xpath);
        assertEquals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE, evaluateXPathResponseBean.getStatus());
        String result = String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<result>%s</result>\n", expectedValue);
        assertEquals(result, evaluateXPathResponseBean.getOutput());
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/caseclaim.xml";
    }

    private void configureQueryMock() throws URISyntaxException {
        URI searchURI = new URI("http://localhost:8000/a/test-1/phone/search/dec220eae9974c788654f23320f3a8d3/?commcare_registry=shubham&case_type=case");
        String searchResponse = "query_responses/case_claim_response.xml";
        when(webClientMock.get(eq(searchURI))).thenReturn(FileUtils.getFile(this.getClass(), searchResponse));

        URI firstQueryURI = new URI("http://localhost:8000/a/test-1/phone/registry_case/dec220eae9974c788654f23320f3a8d3/?commcare_registry=shubham&case_type=case&case_id=0156fa3e-093e-4136-b95c-01b13dae66c6");
        String firstQueryResponse = "query_responses/case_claim_response.xml";
        when(webClientMock.get(eq(firstQueryURI))).thenReturn(FileUtils.getFile(this.getClass(), firstQueryResponse));

        URI secondQueryURI = new URI("http://localhost:8000/a/test-1/phone/registry_case/dec220eae9974c788654f23320f3a8d3/?commcare_registry=shubham&case_type=case&case_id=dupe_case_id");
        String secondQueryResponse = "query_responses/registry_query_response.xml";
        when(webClientMock.get(eq(secondQueryURI))).thenReturn(FileUtils.getFile(this.getClass(), secondQueryResponse));
    }
}
