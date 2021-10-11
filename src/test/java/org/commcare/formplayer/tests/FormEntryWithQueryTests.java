package org.commcare.formplayer.tests;


import org.commcare.formplayer.beans.EvaluateXPathResponseBean;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.beans.menus.QueryResponseBean;
import org.commcare.formplayer.objects.QueryData;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ContextConfiguration;

import java.net.URI;
import java.util.Hashtable;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        EvaluateXPathResponseBean evaluateXPathResponseBean = evaluateXPath(formResponse.getSessionId(),
                "instance('registry')/results/case[@case_type='case']/case_name");
        assert evaluateXPathResponseBean.getStatus().equals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE);
        String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<result>Burt Maclin</result>\n";
        assert evaluateXPathResponseBean.getOutput().equals(result);
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/caseclaim.xml";
    }

    private void configureQueryMock() {
        when(webClientMock.get(any(URI.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "query_responses/case_claim_response.xml"));
    }
}
