package org.commcare.formplayer.tests;


import org.apache.commons.lang3.builder.ToStringBuilder;
import org.commcare.formplayer.beans.EvaluateXPathResponseBean;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.beans.menus.QueryResponseBean;
import org.commcare.formplayer.mocks.casexml.CaseFixtureBlock;
import org.commcare.formplayer.mocks.casexml.CaseFixtureResult;
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
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class EndOfFormNavFormLinkingWithQueryTests extends BaseTestClass{

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

    @Override
    protected String getMockRestoreFileName() {
        return "restores/case_with_usercase.xml";
    }

    /**
     * Test form linking with xpath expression evaluates to true
     */
    @Test
    public void testFormLinkingFromFollowupForm1ToFollowupForm2() throws Exception {
        NewFormResponse response = sessionNavigate(
            new String[]{"0", "first_case", "0"},
            "form-linking-test-with-stack-queries",
            NewFormResponse.class
        );
        assertEquals("1st Followup Form", response.getTitle());
        SubmitResponseBean submitResponse = submitForm(new HashMap<>(), response.getSessionId());
        NewFormResponse formResponse = getNextScreenForEOFNavigation(submitResponse, NewFormResponse.class);
        assertEquals("2nd Followup Form", formResponse.getTitle());
    }

    private void configureQueryMock() throws URISyntaxException {
        URI searchURI = new URI("http://localhost:8000/a/test/phone/search/c4d2d3a7b32948cea64d28e961b183cb/?commcare_registry=test&case_type=case");
        String searchResponse = CaseFixtureResult.blocksToString(
                new CaseFixtureBlock.Builder("case", "first_case")
                        .property("case_name", "first_case")
                        .property("another_case_id", "second_case")
                        .build(),
                new CaseFixtureBlock.Builder("case", "second_case")
                        .property("case_name", "second_case")
                        .build()
        );
        when(webClientMock.get(eq(searchURI))).thenReturn(searchResponse);

        URI firstQueryURI = new URI("http://localhost:8000/a/test/phone/registry_case/c4d2d3a7b32948cea64d28e961b183cb/?commcare_registry=test&case_type=case&case_id=first_case");
        String firstQueryResponse = "query_responses/case_claim_response.xml";
        when(webClientMock.get(eq(firstQueryURI))).thenReturn(searchResponse);

        URI secondQueryURI = new URI("http://localhost:8000/a/test/phone/registry_case/c4d2d3a7b32948cea64d28e961b183cb/?commcare_registry=test&case_type=case&case_id=second_case");
        when(webClientMock.get(eq(secondQueryURI))).thenReturn(searchResponse);
    }
}
