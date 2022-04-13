package org.commcare.formplayer.tests;

import org.apache.commons.io.IOUtils;
import org.commcare.formplayer.exceptions.AlreadyExistsInPoolException;
import org.commcare.formplayer.exceptions.ExceedsMaxPoolSizeException;
import org.commcare.formplayer.exceptions.FormDefEntryNotFoundException;
import org.commcare.formplayer.exceptions.ExceedsMaxPoolSizePerId;
import org.commcare.formplayer.objects.FormDefPool;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.util.XFormUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.InputStreamReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class FormDefPoolTests extends BaseTestClass {

    private FormDefPool formDefPool;
    private FormDef formDefToTest;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.formDefPool = new FormDefPool();
        this.formDefToTest = this.loadFormDef();
    }

    @Test
    public void testCreateThrowsExceptionIfFormDefAlreadyAdded() throws Exception {
        this.formDefPool.create("abc123", this.formDefToTest);
        assertThrows(AlreadyExistsInPoolException.class, () -> this.formDefPool.create("abc123", this.formDefToTest));
    }

    @Test
    public void testCreateThrowsExceptionWhenMaxObjsPerIdIsReached() throws Exception {
        FormDefPool customFormDelPool = new FormDefPool(1, 10);
        customFormDelPool.create("abc123", this.formDefToTest);
        assertThrows(ExceedsMaxPoolSizePerId.class, () -> {
            customFormDelPool.create("abc123", this.formDefToTest);
        });
    }

    @Test
    public void testCreateThrowsExceptionWhenMaxObjsTotalIsReached() throws Exception {
        FormDefPool customFormDelPool = new FormDefPool(1, 1);
        customFormDelPool.create("abc123", this.formDefToTest);
        assertThrows(ExceedsMaxPoolSizeException.class, () -> {
            customFormDelPool.create("def456", this.formDefToTest);
        });
    }

    @Test
    public void testCreateSuccessfulForDifferentCopies() throws Exception {
        FormDef formDefToTest2 = loadFormDef();
        assertNotSame(this.formDefToTest, formDefToTest2);
        try {
            this.formDefPool.create("abc123", this.formDefToTest);
            this.formDefPool.create("abc123", formDefToTest2);
        } catch (AlreadyExistsInPoolException e) {
            fail("Unexpected AlreadyExistsInPoolException thrown");
        }
    }

    @Test
    public void testGetReturnsNullIfNoEntryExists() {
        FormDef actualFormDef = this.formDefPool.getFormDef("abc123");
        assertNull(actualFormDef);
    }

    @Test
    public void testGetSucceedsIfEntryExists() throws Exception {
        this.formDefPool.create("abc123", this.formDefToTest);
        FormDef actualFormDef = this.formDefPool.getFormDef("abc123");
        assertEquals(this.formDefToTest, actualFormDef);
    }

    @Test
    public void testGetReturnsNullIfEntryIsUnavailable() throws Exception {
        this.formDefPool.create("abc123", this.formDefToTest);

        FormDef firstLease = this.formDefPool.getFormDef("abc123");
        FormDef secondLease = this.formDefPool.getFormDef("abc123");

        assertEquals(this.formDefToTest, firstLease);
        assertNull(secondLease);
    }

    @Test
    public void testReturnIsSuccessfulIfEntryCreatedPreviously() throws Exception {
        this.formDefPool.create("abc123", this.formDefToTest);
        FormDef firstLease = this.formDefPool.getFormDef("abc123");
        try {
            this.formDefPool.returnFormDef("abc123", firstLease);
        } catch (FormDefEntryNotFoundException e) {
            fail("Unexpected exception thrown: " + e);
        }

        FormDef postReturnFormDef = this.formDefPool.getFormDef("abc123");

        assertEquals(this.formDefToTest, postReturnFormDef);
    }

    @Test
    public void testReturnThrowsExceptionIfEntryNotCreatedPreviously() {
        assertThrows(FormDefEntryNotFoundException.class, () -> {
            this.formDefPool.returnFormDef("abc123", this.formDefToTest);
        });
    }

    @Test
    public void testReturnsCorrectFormDefForMultipleLikeEntries() throws Exception {
        // NOTE: this also tests the create method for existing entries
        this.formDefPool.create("abc123", this.formDefToTest);
        FormDef formDefToTest2 = this.loadFormDef();
        this.formDefPool.create("abc123", formDefToTest2);

        FormDef formDef1 = this.formDefPool.getFormDef("abc123");
        FormDef formDef2 = this.formDefPool.getFormDef("abc123");

        assertEquals(this.formDefToTest, formDef1);
        assertEquals(formDefToTest2, formDef2);
    }

    @Test
    public void testReturnsCorrectFormDefForMultipleUniqueEntries() throws Exception {
        this.formDefPool.create("abc123", this.formDefToTest);
        FormDef formDefToTest2 = this.loadFormDef();
        this.formDefPool.create("def456", formDefToTest2);

        FormDef formDef1 = this.formDefPool.getFormDef("abc123");
        FormDef formDef2 = this.formDefPool.getFormDef("def456");

        assertEquals(this.formDefToTest, formDef1);
        assertEquals(formDefToTest2, formDef2);
    }

    @Test
    public void testFormDefIsResetAfterUse() throws Exception {
        this.formDefPool.create("abc123", this.formDefToTest);
        FormDef formDef = this.formDefPool.getFormDef("abc123");
        TreeElement cleanRoot = formDef.getMainInstance().getRoot().deepCopy(true);
        this.modifyFormDef(formDef);
        TreeElement dirtyRoot = formDef.getMainInstance().getRoot().deepCopy(true);
        assertNotEquals(cleanRoot, dirtyRoot);

        this.formDefPool.returnFormDef("abc123", formDef);
        FormDef cleanedFormDef = this.formDefPool.getFormDef("abc123");

        assertEquals(cleanRoot, cleanedFormDef.getMainInstance().getRoot());
        // ensure it is a copy
        assertNotSame(cleanRoot, cleanedFormDef.getMainInstance().getRoot());
    }

    private void modifyFormDef(FormDef formDef) throws Exception {
        String instanceContent = "<?xml version='1.0' ?><data uiVersion=\"1\" version=\"5\" name=\"Survey\" xmlns:jrm=\"http://dev.commcarehq.org/jr/xforms\" xmlns=\"http://openrosa.org/formdesigner/962C095E-3AB0-4D92-B9BA-08478FF94475\"><favorite_number /><twice_favorite_number /><n0:meta xmlns:n0=\"http://openrosa.org/jr/xforms\"><n0:deviceID>Formplayer</n0:deviceID><n0:timeStart>2022-04-13T07:43:38.119-07</n0:timeStart><n0:timeEnd /><n0:username>test</n0:username><n0:userID>a8f4c2aea6134d01a9ce5856064299b8</n0:userID><n0:instanceID>5c8f65a8-f80b-474a-91a7-684beaf8d83a</n0:instanceID><n1:appVersion xmlns:n1=\"http://commcarehq.org/xforms\">Formplayer Version: 2.53</n1:appVersion><n0:drift /></n0:meta></data>";
        StringReader stringReader = new StringReader(instanceContent);
        XFormParser.loadXmlInstance(formDef, stringReader);
    }

    private FormDef loadFormDef() throws Exception {
        String formXml = FileUtils.getFile(this.getClass(), "xforms/hidden_value_form.xml");
        return XFormUtils.getFormRaw(
                new InputStreamReader(IOUtils.toInputStream(formXml, "UTF-8")));
    }

}
