package org.commcare.formplayer.tests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.commcare.formplayer.util.FormplayerDateUtils;
import org.commcare.formplayer.utils.TestContext;

/**
 * Tests for FormplayerDateUtils
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class FormplayerDateUtilsTest {
    @Test
    public void testConvertDate() {
        String date = "Mon Oct 17 11:36:50 EDT 2016";
        String iso = FormplayerDateUtils.convertJavaDateStringToISO(date);
        Assert.assertEquals("2016-10-17T15:36:50Z", iso);

        date = "Not a real date";
        iso = FormplayerDateUtils.convertJavaDateStringToISO(date);
        Assert.assertEquals(null, iso);
    }
}
