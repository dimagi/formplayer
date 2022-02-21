package org.commcare.formplayer.tests;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.Multimap;

import org.commcare.formplayer.beans.menus.BaseResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.objects.QueryData;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Hashtable;

/**
 * Tests for smart link workflow
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class SmartLinkTests extends BaseTestClass {

    private static final String SMART_LINK_APP = "smart_link";

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("caseclaimdomain", "caseclaimusername");
        mockDefaultSearchResponse();
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/case_claim_parent_child.xml";
    }

    @Test
    public void testSmartLink() throws Exception {
        BaseResponseBean response = sessionNavigateWithQuery(
                new String[]{"1", "192262cb-fbfa-46a4-ba91-a9d13659b0e1"},
                SMART_LINK_APP,
                null,
                BaseResponseBean.class);
        assert response.getSmartLinkRedirect().contentEquals(
                "https://staging.commcarehq.org/a/X/app/v1/199d5aa3beea4c9dbb1ea002c087a302"
                        + "/songs_endpoint/?case_id=192262cb-fbfa-46a4-ba91-a9d13659b0e1");
    }

    @Test
    public void testSmartLinkWithSearchAgain() throws Exception {
        sessionNavigateWithQuery(
                new String[]{"1"},
                SMART_LINK_APP,
                null,
                EntityListResponse.class);

        mockSearchAgainResponse();
        QueryData queryData = new QueryData();
        queryData.setExecute("search_command.m1", true);
        queryData.setForceManualSearch("search_command.m1", true);
        Hashtable<String, String> inputs = new Hashtable<>();
        inputs.put("mood", "3");
        queryData.setInputs("search_command.m1", inputs);

        sessionNavigateWithQuery(
                new String[]{"1"},
                SMART_LINK_APP,
                queryData,
                EntityListResponse.class);

        BaseResponseBean response = sessionNavigateWithQuery(
                new String[]{"1", "192262cb-fbfa-46a4-ba91-a9d13659b0e2"},
                SMART_LINK_APP,
                queryData,
                BaseResponseBean.class);
        assert response.getSmartLinkRedirect().contentEquals(
                "https://staging.commcarehq.org/a/X/app/v1/199d5aa3beea4c9dbb1ea002c087a302"
                        + "/songs_endpoint/?case_id=192262cb-fbfa-46a4-ba91-a9d13659b0e2");
    }

    private void mockDefaultSearchResponse() {
        when(webClientMock.postFormData(anyString(), argThat(data -> (data != null) &&
                ((Multimap<String, String>)data).get("case_type").contains("song")))).thenReturn(
                FileUtils.getFile(this.getClass(),
                        "query_responses/smart_link_default_response.xml"));
    }

    private void mockSearchAgainResponse() {
        when(webClientMock.postFormData(anyString(), argThat(data -> (data != null) &&
                ((Multimap<String, String>)data).get("case_type").contains("song")))).thenReturn(
                FileUtils.getFile(this.getClass(),
                        "query_responses/smart_link_search_again_response.xml"));
    }
}
