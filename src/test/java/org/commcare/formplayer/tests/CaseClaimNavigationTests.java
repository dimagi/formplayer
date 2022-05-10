package org.commcare.formplayer.tests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.collect.Multimap;

import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.objects.QueryData;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.Assertions;
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

            Assertions.assertEquals(1, entityListResponse.getEntities().length);
            Assertions.assertEquals("0156fa3e-093e-4136-b95c-01b13dae66c6",
                    entityListResponse.getEntities()[0].getId());
        }

        selections.add("0156fa3e-093e-4136-b95c-01b13dae66c6");

        try(
                VerifiedMock ignoredPostMock = mockPost();
                VerifiedMock ignoredRestoreMock = mockRestore("restores/caseclaim3.xml");
        ) {
            CommandListResponseBean commandListResponseBean = sessionNavigateWithQuery(selections,
                    APP_PATH,
                    queryData,
                    CommandListResponseBean.class);
            Assertions.assertEquals(2, commandListResponseBean.getCommands().length);
        }
    }

    /**
     * Mock the post request and verify it happened
     */
    private VerifiedMock mockPost() {
        Mockito.reset(webClientMock);
        when(webClientMock.caseClaimPost(anyString(), any())).thenReturn(true);

        return () -> {
            VerificationMode once = Mockito.times(1);
            Mockito.verify(webClientMock, once).caseClaimPost(anyString(), any());
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
            Mockito.verify(restoreFactoryMock, once).getRestoreXml(anyBoolean());
        };
    }

    /**
     * Mock the query and verify it happened
     */
    private VerifiedMock mockQuery(String queryFile) {
        Mockito.reset(webClientMock);
        when(webClientMock.postFormData(anyString(), any(Multimap.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), queryFile));

        return () -> Mockito.verify(
                webClientMock, Mockito.times(1)).postFormData(
                        anyString(), any(Multimap.class));
    }

    /**
     * Tagging class to make it clearer what is being returned by mock methods.
     */
    private interface VerifiedMock extends AutoCloseable { }
}
