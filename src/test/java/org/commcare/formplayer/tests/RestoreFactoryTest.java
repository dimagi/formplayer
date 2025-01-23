package org.commcare.formplayer.tests;

import static org.commcare.formplayer.util.Constants.TOGGLE_INCLUDE_STATE_HASH;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static java.util.Collections.singletonList;

import org.commcare.cases.util.CaseDBUtils;
import org.commcare.formplayer.auth.DjangoAuth;
import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.commcare.formplayer.configuration.CacheConfiguration;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.RequestUtils;
import org.commcare.formplayer.utils.TestContext;
import org.commcare.formplayer.utils.WithHqUser;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Created by benrudolph on 1/19/17.
 */
@WebMvcTest
@ContextConfiguration(classes = {TestContext.class, CacheConfiguration.class})
public class RestoreFactoryTest {

    private static final String BASE_URL = "http://localhost:8000/a/restore-domain/phone/restore/";
    private String username = "restore-dude";
    private String domain = "restore-domain";
    private String asUsername = "restore-gal";

    @Value("${commcarehq.formplayerAuthKey}")
    private String formplayerAuthKey;

    @Autowired
    RestoreFactory restoreFactorySpy;

    @Mock
    private ServletRequestAttributes requestAttributes;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        Mockito.reset(restoreFactorySpy);
        AuthenticatedRequestBean requestBean = new AuthenticatedRequestBean();
        requestBean.setRestoreAs(asUsername);
        requestBean.setUsername(username);
        requestBean.setDomain(domain);
        restoreFactorySpy.configure(requestBean, new DjangoAuth("key"));
        restoreFactorySpy.setAsUsername(null);
        restoreFactorySpy.setCaseId(null);

        // mock request
        RequestContextHolder.setRequestAttributes(requestAttributes);
        when(requestAttributes.getRequest()).thenReturn(request);
    }

    private void mockHmacRequest() {
        when(request.getAttribute(eq(Constants.HMAC_REQUEST_ATTRIBUTE))).thenReturn(true);
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
        assertEquals(7, headers.size());
        validateHeaders(headers, Arrays.asList(
                hasEntry("Cookie", singletonList("sessionid=key")),
                hasEntry("sessionid", singletonList("key")),
                hasEntry("Authorization", singletonList("sessionid=key")),
                hasEntry("X-OpenRosa-Version", singletonList("3.0")),
                hasEntry("X-OpenRosa-DeviceId", singletonList("WebAppsLogin")),
                hasEntry("X-CommCareHQ-LastSyncToken", singletonList(syncToken)),
                hasEntry(equalTo("X-CommCareHQ-Origin-Token"), new ValueIsUUID()))
        );
    }

    @Test
    public void testGetRequestHeaders_HmacAuth() throws Exception {
        mockHmacRequest();
        restoreFactorySpy.configure(domain, "case_id", null);
        String requestPath = "/a/restore-domain/phone/case_restore/case_id_123/";
        HttpHeaders headers = restoreFactorySpy.getRequestHeaders(
                new URI("http://localhost:8000" + requestPath));
        assertEquals(4, headers.size());
        validateHeaders(headers, Arrays.asList(
                hasEntry("X-MAC-DIGEST",
                        singletonList(RequestUtils.getHmac(formplayerAuthKey, requestPath))),
                hasEntry("X-OpenRosa-Version", singletonList("3.0")),
                hasEntry("X-OpenRosa-DeviceId", singletonList("WebAppsLogin")),
                hasEntry(equalTo("X-CommCareHQ-Origin-Token"), new ValueIsUUID()))
        );
    }

    @Test
    public void testGetRequestHeaders_HmacAuth_UrlWithQuery() throws Exception {
        mockHmacRequest();
        restoreFactorySpy.configure(domain, "case_id", null);
        String requestPath =
                "/a/restore-domain/phone/case_restore/case_id_123/?query_param=true";
        HttpHeaders headers = restoreFactorySpy.getRequestHeaders(
                new URI("http://localhost:8000" + requestPath));
        assertEquals(4, headers.size());
        validateHeaders(headers, Arrays.asList(
                hasEntry("X-MAC-DIGEST",
                        singletonList(RequestUtils.getHmac(formplayerAuthKey, requestPath))),
                hasEntry("X-OpenRosa-Version", singletonList("3.0")),
                hasEntry("X-OpenRosa-DeviceId", singletonList("WebAppsLogin")),
                hasEntry(equalTo("X-CommCareHQ-Origin-Token"), new ValueIsUUID()))
        );
    }

    @Test
    public void testGetRequestHeaders_UseHmacAuthEvenIfHqAuthPresent() throws Exception {
        mockHmacRequest();
        String requestPath = "/a/restore-domain/phone/case_restore/case_id_123/";
        HttpHeaders headers = restoreFactorySpy.getRequestHeaders(
                new URI("http://localhost:8000" + requestPath));
        assertEquals(4, headers.size());
        validateHeaders(headers, Arrays.asList(
                hasEntry("X-MAC-DIGEST",
                        singletonList(RequestUtils.getHmac(formplayerAuthKey, requestPath))),
                hasEntry("X-OpenRosa-Version", singletonList("3.0")),
                hasEntry("X-OpenRosa-DeviceId", singletonList("WebAppsLogin")),
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
