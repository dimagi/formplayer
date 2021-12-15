package org.commcare.formplayer.tests;

import org.apache.commons.io.IOUtils;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.session.FormSession;
import org.commcare.formplayer.util.serializer.FormDefStringSerializer;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.javarosa.core.model.FormDef;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.xform.util.XFormUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class SavedFormDefTest extends BaseTestClass {

    @Test
    public void testFormDefSavedPriorToInitialization() throws Exception {
        // This test ensures a FormDef is saved to a FormSession prior to form def initialization code being run
        // by checking that the instanceXml matches a formDef's instance made from scratch (rather than through the FormSession creation)
        String formXml = FileUtils.getFile(this.getClass(), "xforms/hidden_value_form.xml");
        FormDef expectedFormDef = XFormUtils.getFormRaw(new InputStreamReader(IOUtils.toInputStream(formXml, "UTF-8")));
        byte[] expectedInstanceBytes = new XFormSerializingVisitor(false).serializeInstance(expectedFormDef.getInstance());

        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_3.json",
                "xforms/hidden_value_form.xml");

        SerializableFormSession session = this.formSessionService.getSessionById(newSessionResponse.getSessionId());
        FormDef actualFormDef = FormDefStringSerializer.deserialize(session.getFormXml());
        byte[] actualInstanceBytes = new XFormSerializingVisitor(false).serializeInstance(actualFormDef.getInstance());

        String actualFormDefString = new String(actualInstanceBytes, "US-ASCII");
        String expectedFormDefString = new String(expectedInstanceBytes, "US-ASCII");
        assertEquals(expectedFormDefString, actualFormDefString);
    }

    @Test
    public void testIrrelevantDataIsIncludedPriorToSubmission() throws Exception {
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_3.json",
                "xforms/hidden_value_form.xml");

        String sessionId = newSessionResponse.getSessionId();

        // this answer does not satisfy the condition, and therefore the calculated property is irrelevant
        answerQuestionGetResult("0", "9", sessionId);

        SerializableFormSession session = this.formSessionService.getSessionById(newSessionResponse.getSessionId());
        FormSession formSession = new FormSession(session, this.restoreFactoryMock, null, this.storageFactoryMock, null, this.remoteInstanceFetcherMock);
        assertEquals(formSession.getInstanceXml(true), session.getInstanceXml());
    }

    @Test
    public void testIrrelevantDataIsNotIncludedInSubmission() throws Exception {
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_3.json",
                "xforms/hidden_value_form.xml");

        String sessionId = newSessionResponse.getSessionId();

        // this answer does not satisfy the condition, and therefore the calculated property is irrelevant
        answerQuestionGetResult("0", "9", sessionId);
        SerializableFormSession session = this.formSessionService.getSessionById(newSessionResponse.getSessionId());
        FormSession formSession = new FormSession(session, this.restoreFactoryMock, null, this.storageFactoryMock, null, this.remoteInstanceFetcherMock);
        SubmitResponseBean submitResponseBean = submitForm("requests/submit/submit_hidden_value_form.json", sessionId);
        assertEquals("success", submitResponseBean.getStatus());

        Mockito.verify(this.submitServiceMock).submitForm(formSession.getInstanceXml(false), formSession.getPostUrl());
    }
}
