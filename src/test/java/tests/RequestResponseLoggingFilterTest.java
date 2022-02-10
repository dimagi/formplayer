package tests;

import org.apache.commons.logging.Log;
import org.commcare.formplayer.application.RequestResponseLoggingFilter;
import org.commcare.formplayer.utils.TestContext;
import org.commcare.formplayer.utils.WithHqUser;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class RequestResponseLoggingFilterTest {

    Log log = null;

    @BeforeEach
    public void setUp() throws Exception {
        this.log = mock(Log.class);
    }

    @Test
    @WithHqUser
    public void testDoFilter() throws IOException, ServletException {
        String restoreAs = "user1";
        String requestBody = String.format("{restoreAs: \"%s\"}", restoreAs);
        String responseBody = "response";
        String user = "user";
        String domain = "domain";
        String uri = "/test";

        HttpServletRequest request = this.getHttpRequest(requestBody, uri);
        MockHttpServletResponse response = this.getHttpServletResponse(responseBody);
        FilterChain filterChain = this.getFilterChain(responseBody);

        RequestResponseLoggingFilter reqRespFilter = new RequestResponseLoggingFilter(this.log, true);
        reqRespFilter.doFilter(request, response, filterChain);

        verify(this.log).info(argThat(logMessage -> {
            return ((JSONObject)logMessage).getString("username").equals(user) &&
                    ((JSONObject)logMessage).getString("username").equals(user) &&
                    ((JSONObject)logMessage).getString("requestBody").equals(requestBody) &&
                    ((JSONObject)logMessage).getString("restoreAs").equals(restoreAs) &&
                    ((JSONObject)logMessage).getString("requestUrl").equals("http://localhost" + uri) &&
                    ((JSONObject)logMessage).getString("requestBody").equals(requestBody);

        }));
    }

    @Test
    @WithHqUser
    public void testDoFilterNoLogging() throws IOException, ServletException {
        HttpServletRequest request = this.getHttpRequest("b", "u");
        MockHttpServletResponse response = this.getHttpServletResponse("b");
        FilterChain filterChain = this.getFilterChain("b");

        RequestResponseLoggingFilter reqRespFilter = new RequestResponseLoggingFilter(this.log, false);
        reqRespFilter.doFilter(request, response, filterChain);

        verify(this.log, never()).info(Mockito.any());
    }

    private HttpServletRequest getHttpRequest(String body, String uri) {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setContent(body.getBytes());
        mockHttpServletRequest.setRequestURI(uri);
        return mockHttpServletRequest;
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
