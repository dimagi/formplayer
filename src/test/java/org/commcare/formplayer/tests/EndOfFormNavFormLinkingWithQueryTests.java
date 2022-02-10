package org.commcare.formplayer.tests;


import com.google.common.collect.ImmutableMultimap;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.mocks.casexml.CaseFixtureBlock;
import org.commcare.formplayer.mocks.casexml.CaseFixtureResultSerializer;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ContextConfiguration;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class EndOfFormNavFormLinkingWithQueryTests extends BaseTestClass {

    @Autowired
    CacheManager cacheManager;

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

    private void configureQueryMock() {
        String searchResponse = CaseFixtureResultSerializer.blocksToString(
                new CaseFixtureBlock.Builder("case", "first_case")
                        .withProperty("case_name", "first_case")
                        .withProperty("another_case_id", "second_case")
                        .build(),
                new CaseFixtureBlock.Builder("case", "second_case")
                        .withProperty("case_name", "second_case")
                        .build()
        );
        String searchUrl = "http://localhost:8000/a/test/phone/search/c4d2d3a7b32948cea64d28e961b183cb/";
        ImmutableMultimap<String, String> data = ImmutableMultimap.of("commcare_registry", "test", "case_type", "case");
        when(webClientMock.postFormData(eq(searchUrl), eq(data))).thenReturn(searchResponse);

        String registryUrl = "http://localhost:8000/a/test/phone/registry_case/c4d2d3a7b32948cea64d28e961b183cb/";
        ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
        builder.putAll(data).put("case_id", "first_case");
        when(webClientMock.postFormData(eq(registryUrl), eq(builder.build()))).thenReturn(searchResponse);

        builder = ImmutableMultimap.builder();
        builder.putAll(data).put("case_id", "second_case");
        when(webClientMock.postFormData(eq(registryUrl), eq(builder.build()))).thenReturn(searchResponse);
    }
}
