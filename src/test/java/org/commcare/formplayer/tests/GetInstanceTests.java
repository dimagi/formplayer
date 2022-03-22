package org.commcare.formplayer.tests;

import org.apache.commons.io.IOUtils;
import org.commcare.formplayer.beans.GetInstanceResponseBean;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.tests.BaseTestClass;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.javarosa.core.model.FormDef;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.xform.util.XFormUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for GetInstanceResponseBean
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class GetInstanceTests extends BaseTestClass {

    @Test
    public void testRespectsRelevancy() throws Exception {
        String formXml = FileUtils.getFile(this.getClass(), "xforms/hidden_value_form.xml");
        FormDef expectedFormDef = XFormUtils.getFormRaw(
                new InputStreamReader(IOUtils.toInputStream(formXml, "UTF-8")));
        byte[] expectedInstanceBytes = new XFormSerializingVisitor(false).serializeInstance(
                expectedFormDef.getInstance());

        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_3.json",
                "xforms/hidden_value_form.xml");
        String sessionId = newSessionResponse.getSessionId();

        // do not answer any questions

        GetInstanceResponseBean getInstanceResponse = getInstance(sessionId);

        assertTrue(getInstanceResponse.getOutput().contains("favorite_number"));
        assertFalse(getInstanceResponse.getOutput().contains("twice_favorite_number"));
    }
}
