package org.commcare.formplayer.tests;

import org.apache.commons.io.IOUtils;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.util.serializer.FormDefStringSerializer;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.javarosa.core.model.FormDef;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.xform.util.XFormUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class SavedFormDefTest extends BaseTestClass {


    @Test
    public void testFormDefInstanceEqualityAfterSessionCreation() throws Exception {
        // This test ensures the instanceXml from the formDef saved to the FormSession prior to initialization
        // is the same as the instanceXml off of a freshly created FormDef from the xml file
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_3.json",
                "xforms/cases/create_case.xml");

        String formXml = FileUtils.getFile(this.getClass(), "xforms/cases/create_case.xml");
        FormDef expectedFormDef = XFormUtils.getFormRaw(new InputStreamReader(IOUtils.toInputStream(formXml, "UTF-8")));
        byte[] expectedInstanceBytes = new XFormSerializingVisitor(false).serializeInstance(expectedFormDef.getInstance());


        SerializableFormSession session = this.formSessionService.getSessionById(newSessionResponse.getSessionId());
        FormDef actualFormDef = FormDefStringSerializer.deserialize(session.getFormXml());
        byte[] actualInstanceBytes = new XFormSerializingVisitor(false).serializeInstance(actualFormDef.getInstance());

        String actualFormDefString = new String(actualInstanceBytes, "US-ASCII");
        String expectedFormDefString = new String(expectedInstanceBytes, "US-ASCII");
        assertEquals(expectedFormDefString, actualFormDefString);
    }
}
