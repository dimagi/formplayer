package org.commcare.formplayer.tests;

import org.commcare.formplayer.application.UtilController;
import org.commcare.formplayer.configuration.WebSecurityConfig;
import org.commcare.formplayer.request.MultipleReadRequestWrappingFilter;
import org.commcare.formplayer.services.FormplayerLockRegistry;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.NotificationLogger;
import org.commcare.formplayer.util.RequestUtils;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest
@Import({UtilController.class, MultipleReadRequestWrappingFilter.class})
@ContextConfiguration(classes = {TestContext.class, WebSecurityConfig.class})
public class FormValidatorAuthTests {

    private MediaType contentType = new MediaType(MediaType.APPLICATION_XML.getType(),
            MediaType.APPLICATION_XML.getSubtype(),
            Charset.forName("utf8"));

    @Autowired
    private MockMvc mvc;

    @MockBean
    private FormplayerLockRegistry lockRegistry;

    @MockBean
    private NotificationLogger notificationLogger;

    @Value("${commcarehq.formplayerAuthKey}")
    private String formplayerAuthKey;

    @Test
    public void testValidateFormWithHmacAuth_Succeeds() throws Exception {
        String xml = FileUtils.getFile(this.getClass(), "form_validation/valid_form.xml");
        this.testValidateForm(xml, Arrays.asList(
            jsonPath("$.validated", is(true)),
            jsonPath("$.problems", hasSize(0)),
            status().isOk()
        ), true);
    }

    @Test
    public void testValidateFormWithoutAuth_Fails() throws Exception {
        String xml = FileUtils.getFile(this.getClass(), "form_validation/valid_form.xml");
        this.testValidateForm(xml, Collections.singletonList(status().isForbidden()), false);
    }

    public void testValidateForm(String formXML, List<ResultMatcher> matchers, boolean withAuth) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(String.format("/%s", Constants.URL_VALIDATE_FORM))
            .content(formXML)
            .contentType(contentType);

        if (withAuth) {
            String hmac = RequestUtils.getHmac(formplayerAuthKey, formXML);
            requestBuilder = requestBuilder.header(Constants.HMAC_HEADER, hmac);
         }

        ResultActions actions = mvc.perform(requestBuilder)
                .andDo(log());

        for (ResultMatcher matcher : matchers   ) {
            actions = actions.andExpect(matcher);
        }
    }
}
