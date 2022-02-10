package org.commcare.formplayer.tests;

import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class FormValidatorTests extends BaseTestClass {

    private MediaType contentType = new MediaType(MediaType.APPLICATION_XML.getType(),
            MediaType.APPLICATION_XML.getSubtype(),
            Charset.forName("utf8"));

    @Test
    public void testValidateForm() throws Exception {
        String xml = FileUtils.getFile(this.getClass(), "form_validation/valid_form.xml");
        this.testValidateForm(xml, Arrays.asList(
                jsonPath("$.validated", is(true)),
                jsonPath("$.problems", hasSize(0))
        ));
    }

    @Test
    public void testValidateFormNotXML() throws Exception {
        String xml = "this isn't XML";
        this.testValidateForm(xml, Arrays.asList(
                jsonPath("$.problems", hasSize(0)),
                jsonPath("$.validated", is(false))
        ));
    }

    @Test
    public void testValidateFormBadRef() throws Exception {
        String xml = FileUtils.getFile(this.getClass(), "form_validation/bad_ref.xml");
        this.testValidateForm(xml, Arrays.asList(
                jsonPath("$.validated", is(false)),
                jsonPath("$.problems", hasSize(1)),
                jsonPath("$.problems[0].message", containsString("/data/missing"))
        ));
    }

    @Test
    public void testValidateFormDependencyCycle() throws Exception {
        String xml = FileUtils.getFile(this.getClass(), "form_validation/cycle.xml");
        this.testValidateForm(xml, Arrays.asList(
                jsonPath("$.validated", is(false)),
                jsonPath("$.problems", hasSize(1)),
                jsonPath("$.problems[0].message", containsString("/data/hidden1")),
                jsonPath("$.problems[0].message", containsString("/data/hidden2"))
        ));
    }

    public void testValidateForm(String formXML, List<ResultMatcher> matchers) throws Exception {
        ResultActions actions = mockUtilController.perform(post(String.format("/%s", Constants.URL_VALIDATE_FORM))
                .content(formXML)
                .contentType(contentType))
                .andExpect(status().isOk())
                .andDo(log());

        for (ResultMatcher matcher : matchers) {
            actions = actions.andExpect(matcher);
        }
    }


}
