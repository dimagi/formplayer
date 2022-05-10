package org.commcare.formplayer.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Multimap;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.objects.QueryData;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;

@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class CaseClaimNavigationTests extends BaseTestClass {

    private static final String APP_PATH = "archives/caseclaim";

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("caseclaimdomain", "caseclaimusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/caseclaim.xml";
    }


    @Test
    public void testNormalClaim() throws Exception {
        ArrayList<String> selections = new ArrayList<>();
        selections.add("1");  // select menu
        selections.add("action 1");  // load case search

        QueryData queryData = new QueryData();
        queryData.setExecute("case_search.m1", true);

        try (VerifiedMock ignore = mockQuery("query_responses/case_claim_response.xml")) {
            EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                    APP_PATH,
                    queryData,
                    EntityListResponse.class);

            assertEquals(1, entityListResponse.getEntities().length);
            assertEquals("0156fa3e-093e-4136-b95c-01b13dae66c6",
                    entityListResponse.getEntities()[0].getId());
        }

        selections.add("0156fa3e-093e-4136-b95c-01b13dae66c6");

        try(
                VerifiedMock ignoredPostMock = mockPost(true);
                VerifiedMock ignoredRestoreMock = mockRestore("restores/caseclaim3.xml");
        ) {
            CommandListResponseBean commandListResponseBean = sessionNavigateWithQuery(selections,
                    APP_PATH,
                    queryData,
                    CommandListResponseBean.class);
            assertEquals(2, commandListResponseBean.getCommands().length);
        }
    }

    @Test
    public void testPostInEntry() throws Exception {
        ArrayList<String> selections = new ArrayList<>();
        selections.add("2");  // m2
        selections.add("0");  // 1st form

        QueryData queryData = new QueryData();

        EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                APP_PATH,
                queryData,
                EntityListResponse.class);

        assertThat(entityListResponse.getEntities()).anyMatch(e -> {
            return e.getId().equals("56306779-26a2-4aa5-a952-70c9d8b21e39");
        });

        verifyNoInteractions(webClientMock);  // post should only be executed once form is selected

        selections.add("56306779-26a2-4aa5-a952-70c9d8b21e39");
        try(VerifiedMock ignored = mockPost(false)) {
            sessionNavigateWithQuery(selections,
                    APP_PATH,
                    queryData,
                    NewFormResponse.class);
        }
    }

    @Test
    public void testPostInEntryWithQuery_RelevantFalse() throws Exception {
        ArrayList<String> selections = new ArrayList<>();
        selections.add("2");  // m2
        selections.add("1");  // 2nd form

        QueryData queryData = new QueryData();

        try(VerifiedMock ignored = mockQuery("query_responses/case_claim_response_owned.xml")) {
            EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                    APP_PATH,
                    queryData,
                    EntityListResponse.class);

            assertThat(entityListResponse.getEntities()).anyMatch(e -> {
                return e.getId().equals("3512eb7c-7a58-4a95-beda-205eb0d7f163");
            });
        }

        Mockito.reset(webClientMock);
        selections.add("3512eb7c-7a58-4a95-beda-205eb0d7f163");
        sessionNavigateWithQuery(selections,
                APP_PATH,
                queryData,
                NewFormResponse.class);
        // post should not be fired due to relevant condition evaluating to false
        verifyNoInteractions(webClientMock);
    }

    @Test
    public void testPostInEntryWithQuery_RelevantTrue() throws Exception {
        ArrayList<String> selections = new ArrayList<>();
        selections.add("2");  // m2
        selections.add("1");  // 2nd form

        QueryData queryData = new QueryData();

        try (VerifiedMock ignored = mockQuery("query_responses/case_claim_response.xml")) {
            EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                    APP_PATH,
                    queryData,
                    EntityListResponse.class);

            assertThat(entityListResponse.getEntities()).anyMatch(e -> {
                return e.getId().equals("0156fa3e-093e-4136-b95c-01b13dae66c6");
            });
        }

        selections.add("0156fa3e-093e-4136-b95c-01b13dae66c6");
        NewFormResponse formResponse;
        try (
                VerifiedMock ignoredPostMock = mockPost(true);
                VerifiedMock ignoredRestoreMock = mockRestore("restores/caseclaim3.xml");
        ) {
            formResponse = sessionNavigateWithQuery(selections,
                    APP_PATH,
                    queryData,
                    NewFormResponse.class);
        }

        // case was included in restore and is now in the case DB
        checkXpath(
                formResponse.getSessionId(),
                "count(instance('casedb')/casedb/case[@case_id='0156fa3e-093e-4136-b95c-01b13dae66c6'])",
                "1"
        );
    }

    /**
     * Mock the post request and verify it happened
     */
    private VerifiedMock mockPost(boolean returnValue) {
        Mockito.reset(webClientMock);
        when(webClientMock.caseClaimPost(anyString(), any())).thenReturn(returnValue);

        return () -> {
            VerificationMode once = Mockito.times(1);
            verify(webClientMock, once).caseClaimPost(anyString(), any());
        };
    }

    /**
     * Mock the restore and verify it happened
     */
    private VerifiedMock mockRestore(String restoreFile) {
        Mockito.reset(restoreFactoryMock);
        RestoreFactoryAnswer answer = new RestoreFactoryAnswer(restoreFile);
        Mockito.doAnswer(answer).when(restoreFactoryMock).getRestoreXml(anyBoolean());

        return () -> {
            VerificationMode once = Mockito.times(1);
            verify(restoreFactoryMock, once).getRestoreXml(anyBoolean());
        };
    }

    /**
     * Mock the query and verify it happened
     */
    private VerifiedMock mockQuery(String queryFile) {
        Mockito.reset(webClientMock);
        when(webClientMock.postFormData(anyString(), any(Multimap.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), queryFile));

        return () -> verify(
                webClientMock, Mockito.times(1)).postFormData(
                        anyString(), any(Multimap.class));
    }

    /**
     * Tagging class to make it clearer what is being returned by mock methods.
     */
    private interface VerifiedMock extends AutoCloseable { }
}
