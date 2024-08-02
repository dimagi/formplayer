package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMultimap;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.beans.menus.QueryResponseBean;
import org.commcare.formplayer.objects.QueryData;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.session.FormSession;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.MockRequestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cache.CacheManager;

import java.util.Hashtable;

@WebMvcTest
public class ScreenSizeTest extends BaseTestClass {


    @Autowired
    CacheManager cacheManager;

    private MockRequestUtils mockRequest;


    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("caseclaimdomain", "caseclaimusername");
        cacheManager.getCache("case_search").clear();
        mockRequest = new MockRequestUtils(webClientMock, restoreFactoryMock);
    }

    @Test
    public void testWindowWidth() throws Exception {
        configureQueryMock();
        // selecting the command containing form with `query` should launch the query screen
        sessionNavigateWithQuery(new String[]{"1"},
                "caseclaimquery",
                null,
                QueryResponseBean.class);

        Hashtable<String, String> inputs = new Hashtable<>();
        QueryData queryData = new QueryData();
        queryData.setInputs("m1_results", inputs);
        queryData.setExecute("m1_results", true);

        sessionNavigateWithQuery(new String[]{"1"},
                "caseclaimquery",
                queryData,
                EntityListResponse.class);

        // Open the form with `query` blocks
        NewFormResponse formResponse = sessionNavigateWithQuery(
                new String[]{"1", "0156fa3e-093e-4136-b95c-01b13dae66c6", "0"},
                "caseclaimquery",
                queryData,
                NewFormResponse.class);


        SerializableFormSession serializableFormSession = formSessionService.getSessionById(
                formResponse.getSessionId());
        FormSession formSession = getFormSession(serializableFormSession);
        assertNotNull(formSession.getWindowWidth());
        assertEquals("1796", formSession.getWindowWidth());
    }

    private void configureQueryMock() {
        String searchUri =
                "http://localhost:8000/a/test-1/phone/search/dec220eae9974c788654f23320f3a8d3/";
        String searchResponse = "query_responses/case_claim_response.xml";
        ImmutableMultimap<String, String> data = ImmutableMultimap.of("commcare_registry",
                "shubham", "case_type", "case");
        when(webClientMock.postFormData(eq(searchUri), eq(data))).thenReturn(
                FileUtils.getFile(this.getClass(), searchResponse));

        String registryUrl =
                "http://localhost:8000/a/test-1/phone/registry_case"
                        + "/dec220eae9974c788654f23320f3a8d3/";
        ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
        builder.putAll(data).put("case_id", "0156fa3e-093e-4136-b95c-01b13dae66c6");
        String firstQueryResponse = "query_responses/case_claim_response.xml";
        when(webClientMock.postFormData(eq(registryUrl), eq(builder.build()))).thenReturn(
                FileUtils.getFile(this.getClass(), firstQueryResponse));

        builder = ImmutableMultimap.builder();
        builder.putAll(data).put("case_id", "dupe_case_id");
        String secondQueryResponse = "query_responses/registry_query_response.xml";
        when(webClientMock.postFormData(eq(registryUrl), eq(builder.build()))).thenReturn(
                FileUtils.getFile(this.getClass(), secondQueryResponse));
    }
}
