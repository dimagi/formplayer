package tests;

import application.Application;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.context.WebApplicationContext;
import util.Constants;
import utils.FileUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class FormValidatorTests {

    private MediaType contentType = new MediaType(MediaType.APPLICATION_XML.getType(),
            MediaType.APPLICATION_XML.getSubtype(),
            Charset.forName("utf8"));

    private MockMvc mockMvc;

    private HttpMessageConverter string2HttpMessageConverter;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {
        Optional<HttpMessageConverter<?>> converterOptional = Iterables.tryFind(Arrays.asList(converters), new Predicate<HttpMessageConverter<?>>() {
            @Override
            public boolean apply(HttpMessageConverter<?> hmc) {
                return hmc instanceof StringHttpMessageConverter;
            }
        });

        assertTrue("the XML message converter must not be null", converterOptional.isPresent());
        this.string2HttpMessageConverter = converterOptional.get();
    }


    @Before
    public void setup() throws Exception {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

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
        ResultActions actions = mockMvc.perform(post(String.format("/%s", Constants.URL_VALIDATE_FORM))
                .content(this.xml(formXML))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andDo(log());

        for (ResultMatcher matcher : matchers   ) {
            actions = actions.andExpect(matcher);
        }
    }

    protected String xml(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        this.string2HttpMessageConverter.write(
                o, MediaType.APPLICATION_XML, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

}