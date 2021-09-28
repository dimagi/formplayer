package org.commcare.formplayer.tests;


import org.commcare.formplayer.beans.EvaluateXPathResponseBean;
import org.commcare.formplayer.beans.NewFormResponse;
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
        NewFormResponse responseBean = sessionNavigateWithQuery(new String[]{"0"},
                "caseclaim",
                null,
                false,
                NewFormResponse.class);

        // Check if form's query was executed
        verify(webClientMock, times(1)).get(uriCaptor.capture());
        List<URI> uris = uriCaptor.getAllValues();
        // when default search, prompts doesn't get included
        assert uris.get(0).equals(new URI("http://www.example.com/a/domain/phone/get_case/?case_type=case"));

        // see if the instance is retained into the form session
        EvaluateXPathResponseBean evaluateXPathResponseBean = evaluateXPath(responseBean.getSessionId(),
                "instance('registry')/results/case[@case_type='case']/case_name");
        assert evaluateXPathResponseBean.getStatus().equals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE);
        String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<result>Burt Maclin</result>\n";
        assert evaluateXPathResponseBean.getOutput().equals(result);
    }

    private void configureQueryMock() {
        when(webClientMock.get(any(URI.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "query_responses/case_claim_response.xml"));
    }
}
