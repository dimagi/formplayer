package org.commcare.formplayer.tests;

import org.commcare.cases.util.CaseDBUtils;
import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.commcare.formplayer.configuration.CacheConfiguration;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.utils.TestContext;
import org.commcare.formplayer.utils.WithHqUser;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.commcare.formplayer.util.Constants.TOGGLE_INCLUDE_STATE_HASH;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

/**
 * Created by benrudolph on 1/19/17.
 */
@WebMvcTest
@ContextConfiguration(classes = {TestContext.class, CacheConfiguration.class})
@ExtendWith(MockitoExtension.class)
public class RestoreFactoryTest {

    private static final String BASE_URL = "http://localhost:8000/a/restore-domain/phone/restore/";

    @Autowired
    RestoreFactory restoreFactorySpy;

    @BeforeEach
    public void setUp() throws Exception {
        Mockito.reset(restoreFactorySpy);
        AuthenticatedRequestBean requestBean = new AuthenticatedRequestBean();
        requestBean.setRestoreAs("restore-gal");
        requestBean.setUsername("restore-dude");
        requestBean.setDomain("restore-domain");
        restoreFactorySpy.configure(requestBean);
        restoreFactorySpy.setAsUsername(null);
        restoreFactorySpy.setCaseId(null);
    }

    private void mockSyncFreq(String freq) {
        Mockito.doReturn(freq)
                .when(restoreFactorySpy)
                .getSyncFreqency();
    }

    private void mockLastSyncTime(Long epoch) {
        Mockito.doReturn(epoch)
                .when(restoreFactorySpy)
                .getLastSyncTime();
    }

    @Test
    public void testIsRestoreXmlExpiredDaily() {
        mockSyncFreq(RestoreFactory.FREQ_DAILY);

        // Last sync time should be more than a day ago
        mockLastSyncTime(System.currentTimeMillis() - RestoreFactory.ONE_DAY_IN_MILLISECONDS - 1);

        Assertions.assertTrue(restoreFactorySpy.isRestoreXmlExpired());
    }

    @Test
    public void testNotIsRestoreXmlExpiredDaily() {
        mockSyncFreq(RestoreFactory.FREQ_DAILY);

        // Last sync time should be less than a day ago
        mockLastSyncTime(System.currentTimeMillis());
        Assertions.assertFalse(restoreFactorySpy.isRestoreXmlExpired());
    }

    @Test
    public void testNullIsRestoreXmlExpired() {
        mockSyncFreq("default-case");

        mockLastSyncTime(null);
        Assertions.assertFalse(restoreFactorySpy.isRestoreXmlExpired());
    }

    @Test
    public void testIsRestoreXmlExpiredWeekly() {
        mockSyncFreq(RestoreFactory.FREQ_WEEKLY);

        // Last sync time should be more than a week ago
        mockLastSyncTime(System.currentTimeMillis() - RestoreFactory.ONE_WEEK_IN_MILLISECONDS - 1);

        Assertions.assertTrue(restoreFactorySpy.isRestoreXmlExpired());
    }

    @Test
    public void testNotIsRestoreXmlExpiredWeekly() {
        mockSyncFreq(RestoreFactory.FREQ_WEEKLY);

        // Last sync time should be less than a week ago
        mockLastSyncTime(System.currentTimeMillis());

        Assertions.assertFalse(restoreFactorySpy.isRestoreXmlExpired());
    }

    @Test
    public void testGetCaseRestoreUrl() {
        restoreFactorySpy.setCaseId("case_id_123");
        assertEquals(
                "http://localhost:8000/a/restore-domain/phone/case_restore/case_id_123/",
                restoreFactorySpy.getCaseRestoreUrl().toString()
        );
    }

    @Test
    public void testGetUserRestoreUrl() {
        assertEquals(
                BASE_URL + "?version=2.0&device_id=WebAppsLogin",
                restoreFactorySpy.getUserRestoreUrl(false).toString()
        );
    }

    @Test
    public void testGetUserRestoreUrlWithLoginAs() {
        restoreFactorySpy.setAsUsername("asUser@domain1.commcarehq.org");
        assertEquals(
                BASE_URL + "?version=2.0" +
                        "&device_id=WebAppsLogin%2Arestore-dude%2Aas%2AasUser%40domain1"
                        + ".commcarehq.org"
                        +
                        "&as=asUser%40domain1.commcarehq.org",
                restoreFactorySpy.getUserRestoreUrl(false).toString()
        );
    }

    @Test
    public void testGetUserRestoreUrlWithLoginAs_encoded() {
        restoreFactorySpy.setAsUsername("asUser+test-encoding@domain1.commcarehq.org");
        assertEquals(
                BASE_URL + "?version=2.0" +
                        "&device_id=WebAppsLogin%2Arestore-dude%2Aas%2AasUser%2Btest-encoding"
                        + "%40domain1.commcarehq.org"
                        +
                        "&as=asUser%2Btest-encoding%40domain1.commcarehq.org",
                restoreFactorySpy.getUserRestoreUrl(false).toString()
        );
    }


    @Test
    public void testGetUserRestoreUrlWithLoginAsNoDomain() {
        restoreFactorySpy.setAsUsername("asUser");
        assertEquals(
                BASE_URL + "?version=2.0" +
                        "&device_id=WebAppsLogin%2Arestore-dude%2Aas%2AasUser" +
                        "&as=asUser%40restore-domain.commcarehq.org",
                restoreFactorySpy.getUserRestoreUrl(false).toString()
        );
    }

    @Test
    @WithHqUser(enabledToggles = {TOGGLE_INCLUDE_STATE_HASH})
    public void testGetUserRestoreUrlWithStateHash() {
        try (MockedStatic<CaseDBUtils> mockUtils = Mockito.mockStatic(CaseDBUtils.class)) {
            mockUtils.when(() -> CaseDBUtils.computeCaseDbHash(any())).thenReturn("123");
            assertEquals(
                    BASE_URL + "?version=2.0" +
                            "&device_id=WebAppsLogin" +
                            "&state=ccsh%3A123",
                    restoreFactorySpy.getUserRestoreUrl(false).toString()
            );
        }
    }

    @Test
    public void testGetUserRestoreUrlWithStateHash_toggleOff() {
        try (MockedStatic<CaseDBUtils> mockUtils = Mockito.mockStatic(CaseDBUtils.class)) {
            mockUtils.when(() -> CaseDBUtils.computeCaseDbHash(any())).thenReturn("123");
            assertEquals(
                    BASE_URL + "?version=2.0" +
                            "&device_id=WebAppsLogin",
                    restoreFactorySpy.getUserRestoreUrl(false).toString()
            );
        }
    }

    @Test
    @WithHqUser(enabledToggles = {TOGGLE_INCLUDE_STATE_HASH})
    public void testGetUserRestoreUrlEmptyStateHash() {
        try (MockedStatic<CaseDBUtils> mockUtils = Mockito.mockStatic(CaseDBUtils.class)) {
            mockUtils.when(() -> CaseDBUtils.computeCaseDbHash(any())).thenReturn("");
            assertEquals(
                    BASE_URL + "?version=2.0&device_id=WebAppsLogin",
                    restoreFactorySpy.getUserRestoreUrl(false).toString()
            );
        }
    }

    @Test
    public void testGetRequestHeaders() {
        String syncToken = "synctoken";
        Mockito.doReturn(syncToken).when(restoreFactorySpy).getSyncToken();
        HttpHeaders headers = restoreFactorySpy.getRequestHeaders(null);
        assertEquals(4, headers.size());
        validateHeaders(headers, Arrays.asList(
                hasEntry("X-OpenRosa-Version", singletonList("3.0")),
                hasEntry("X-OpenRosa-DeviceId", singletonList("WebAppsLogin")),
                hasEntry("X-CommCareHQ-LastSyncToken", singletonList(syncToken)),
                hasEntry(equalTo("X-CommCareHQ-Origin-Token"), new ValueIsUUID()))
        );
    }

    private void validateHeaders(HttpHeaders headers,
            List<Matcher<Map<? extends String, ? extends List<String>>>> matchers) {
        for (Matcher<Map<? extends String, ? extends List<String>>> matcher : matchers) {
            MatcherAssert.assertThat(headers, matcher);
        }
    }

    private static class ValueIsUUID extends TypeSafeMatcher<List<String>> {

        @Override
        public void describeTo(Description description) {
            description.appendText("<[A valid UUID string]>");
        }

        @Override
        protected boolean matchesSafely(List<String> item) {
            if (item.size() != 1) {
                return false;
            }
            try {
                UUID.fromString(item.get(0));
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
