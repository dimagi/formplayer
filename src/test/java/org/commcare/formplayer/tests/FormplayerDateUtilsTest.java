package org.commcare.formplayer.tests;

import org.commcare.formplayer.util.FormplayerDateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FormplayerDateUtilsTest {
    @Test
    public void testConvertDate() {
        String date = "Mon Oct 17 11:36:50 EDT 2016";
        String iso = FormplayerDateUtils.convertJavaDateStringToISO(date);
        Assertions.assertEquals("2016-10-17T15:36:50Z", iso);

        date = "Not a real date";
        iso = FormplayerDateUtils.convertJavaDateStringToISO(date);
        Assertions.assertEquals(null, iso);
    }
}
