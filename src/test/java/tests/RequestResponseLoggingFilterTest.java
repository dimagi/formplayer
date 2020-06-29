package tests;

import org.commcare.formplayer.application.RequestResponseLoggingFilter;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.apache.commons.logging.Log;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.commcare.formplayer.util.FormplayerHttpRequest;
import org.commcare.formplayer.utils.TestContext;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class RequestResponseLoggingFilterTest {

    Log log = null;

    @Before
    public void setUp() throws Exception {
        this.log = mock(Log.class);
    }

    @Test
    public void testDoFilter() throws IOException, ServletException {
        String restoreAs = "user1";
        String requestBody = String.format("{restoreAs: \"%s\"}", restoreAs);
        String responseBody = "response";
        String user = "user";
        String domain = "domain";
        String uri = "/test";

        FormplayerHttpRequest request = this.getFormPlayerHttpRequest(requestBody, user, domain, uri);
        MockHttpServletResponse response = this.getHttpServletResponse(responseBody);
        FilterChain filterChain = this.getFilterChain(responseBody);

        RequestResponseLoggingFilter reqRespFilter= new RequestResponseLoggingFilter(this.log, true);
        reqRespFilter.doFilter(request, response, filterChain);

        verify(this.log).info(argThat(logMessage -> {
            return ((JSONObject) logMessage).getString("username").equals(user) &&
                    ((JSONObject) logMessage).getString("username").equals(user) &&
                    ((JSONObject) logMessage).getString("requestBody").equals(requestBody) &&
                    ((JSONObject) logMessage).getString("restoreAs").equals(restoreAs) &&
                    ((JSONObject) logMessage).getString("requestUrl").equals("http://localhost" + uri) &&
                    ((JSONObject) logMessage).getString("requestBody").equals(requestBody);

        }));
    }

    @Test
    public void testDoFilterNoLogging() throws IOException, ServletException {
        FormplayerHttpRequest request = this.getFormPlayerHttpRequest("b", "u", "b", "u");
        MockHttpServletResponse response = this.getHttpServletResponse("b");
        FilterChain filterChain = this.getFilterChain("b");

        RequestResponseLoggingFilter reqRespFilter= new RequestResponseLoggingFilter(this.log, false);
        reqRespFilter.doFilter(request, response, filterChain);

        verify(this.log, never()).info(Mockito.any());
    }

    private FormplayerHttpRequest getFormPlayerHttpRequest(String body, String user, String domain,
                                                           String uri) {
        // Mock formplayerhttprequest with all content and user settings.
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setContent(body.getBytes());
        mockHttpServletRequest.setRequestURI(uri);
        FormplayerHttpRequest request = new FormplayerHttpRequest(mockHttpServletRequest);
        request.setUserDetails(new HqUserDetailsBean(new String[] {domain}, user, false));
        request.setDomain(domain);
        return request;
    }

    private FilterChain getFilterChain(String responseBody) throws IOException, ServletException {
        // Return mock filter chain, but also intercept calls to doFilter (which calls the next filter
        // and eventually the servlet code) to add the response body to the response object.
        FilterChain filterChain = mock(FilterChain.class);
        doAnswer(invocation -> {
            HttpServletResponse servletResponse = invocation.getArgument(1);
            servletResponse.getOutputStream().write(responseBody.getBytes());
            return null;
        }).when(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        return filterChain;
    }

    private MockHttpServletResponse getHttpServletResponse(String body) throws IOException {
        return new MockHttpServletResponse();
    }

}
