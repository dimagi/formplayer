package org.commcare.formplayer.auth;

import static org.commcare.formplayer.auth.AuthTestUtils.getFullAuthRequestBuilder;
import static org.commcare.formplayer.auth.AuthTestUtils.getMultipartRequestBuilder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.commcare.formplayer.application.UtilController;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.configuration.CacheConfiguration;
import org.commcare.formplayer.configuration.WebSecurityConfig;
import org.commcare.formplayer.request.MultipleReadRequestWrappingFilter;
import org.commcare.formplayer.services.FormplayerLockRegistry;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.RequestUtils;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.commcare.formplayer.utils.WithHqUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.Charset;


@WebMvcTest
@ContextConfiguration(classes = {
        UtilController.class,
        MockMultipartController.class,
        TestContext.class,
        WebSecurityConfig.class,
        MultipleReadRequestWrappingFilter.class,
        CacheConfiguration.class
})
public class HmacAuthTests {

    private static final String FULL_AUTH_BODY =
            "{\"username\": \"citrus\", \"domain\":\"swallowtail\"}";
    private MediaType contentType = new MediaType(MediaType.APPLICATION_XML.getType(),
            MediaType.APPLICATION_XML.getSubtype(),
            Charset.forName("utf8"));

    @Autowired
    private MockMvc mvc;

    @MockBean
    private FormplayerLockRegistry lockRegistry;

    @Value("${commcarehq.formplayerAuthKey}")
    private String formplayerAuthKey;

    private static String formXML;

    @Test
    public void testRelaxedEndpoint_WithHmacAuth_Succeeds() throws Exception {
        String hmac = RequestUtils.getHmac(formplayerAuthKey, formXML);
        MockHttpServletRequestBuilder builder = getRelaxedEndpointRequestBuilder()
                .header(Constants.HMAC_HEADER, hmac);
        this.testEndpoint(builder,
                status().isOk(),
                jsonPath("$.validated", is(true)),
                jsonPath("$.problems", hasSize(0))
        );
    }

    @Test
    public void testRelaxedEndpoint_WithoutAnyAuth_Fails() throws Exception {
        this.testEndpoint(getRelaxedEndpointRequestBuilder(), status().isForbidden());
    }

    @Test
    @WithHqUser
    public void testRelaxedEndpoint_WithUserAuth_Succeeds() throws Exception {
        MockHttpServletRequestBuilder builder = getRelaxedEndpointRequestBuilder()
                .with(SecurityMockMvcRequestPostProcessors.csrf());

        this.testEndpoint(builder,
                jsonPath("$.validated", is(true)),
                jsonPath("$.problems", hasSize(0)),
                status().isOk()
        );
    }

    @Test
    public void testFullAuthEndpoint_WithoutAnyAuth_Fails() throws Exception {
        this.testEndpoint(getFullAuthRequestBuilder(FULL_AUTH_BODY), status().isForbidden());
    }

    /**
     * HMAC header is correct but request does not contain user details
     */
    @Test
    public void testFullAuthEndpoint_WithPlainHmac_Fails() throws Exception {
        String hmac = RequestUtils.getHmac(formplayerAuthKey, "{}");
        MockHttpServletRequestBuilder builder = getFullAuthRequestBuilder("{}")
                .header(Constants.HMAC_HEADER, hmac);
        this.testEndpoint(builder, status().isForbidden());
    }

    @Test
    public void testFullAuthEndpoint_WithHmacAuthAndUserDetails_Succeeds() throws Exception {
        String hmac = RequestUtils.getHmac(formplayerAuthKey, FULL_AUTH_BODY);
        MockHttpServletRequestBuilder builder = getFullAuthRequestBuilder(FULL_AUTH_BODY)
                .header(Constants.HMAC_HEADER, hmac);
        this.testEndpoint(builder, status().isOk());
    }

    @Test
    public void testFullAuthMultipartEndpointWithHmac_WithoutSessionAuth_AlwaysFails() throws Exception {
        MockHttpServletRequestBuilder builder = getMultipartRequestBuilder(getClass(), FULL_AUTH_BODY);
        MockHttpServletRequest request = builder.buildRequest(new MockServletContext());
        String hmac = RequestUtils.getHmac(formplayerAuthKey, RequestUtils.getBody(request.getInputStream()));
        builder.header(Constants.HMAC_HEADER, hmac);
        this.testEndpoint(builder, status().isForbidden());
    }

    private void testEndpoint(MockHttpServletRequestBuilder requestBuilder,
            ResultMatcher... matchers) throws Exception {
        ResultActions actions = mvc.perform(requestBuilder)
                .andDo(log());

        for (ResultMatcher matcher : matchers) {
            actions = actions.andExpect(matcher);
        }
    }

    @BeforeAll
    private static void loadXML() {
        formXML = FileUtils.getFile(HmacAuthTests.class, "form_validation/valid_form.xml");
    }

    /**
     * Use the 'validate_form' endpoint for 'relaxed auth' which only requires the HMAC header but
     * not any user details.
     */
    private MockHttpServletRequestBuilder getRelaxedEndpointRequestBuilder() {
        return post(String.format("/%s", Constants.URL_VALIDATE_FORM))
                .content(formXML)
                .contentType(contentType);
    }
}
