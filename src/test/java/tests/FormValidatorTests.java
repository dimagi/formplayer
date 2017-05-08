package tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import util.Constants;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class FormValidatorTests extends BaseTestClass {

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
        ResultActions actions = doValidate(formXML);

        for (ResultMatcher matcher : matchers   ) {
            actions = actions.andExpect(matcher);
        }
    }
}