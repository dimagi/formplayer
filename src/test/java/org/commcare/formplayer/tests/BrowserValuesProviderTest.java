package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.commcare.formplayer.services.BrowserValuesProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;
import java.util.TimeZone;

@ExtendWith(SpringExtension.class)
public class BrowserValuesProviderTest {

    static final String NY_TZ_ID = "America/New_York";
    static final TimeZone NY_TZ = TimeZone.getTimeZone(NY_TZ_ID);
    static final int NY_DST_TZ_OFFSET = -14400000;

    BrowserValuesProvider browserValuesProvider = null;
    Date date = null;

    @BeforeEach
    public void setUp() throws Exception {
        this.browserValuesProvider = new BrowserValuesProvider();
        this.date = new Date(1585699200000L); // April 1, 2020 12:00:00 AM
    }

    @Test
    public void testCheckTzDiscrepancy() throws Exception {
        // Should not throw an exception.
        AuthenticatedRequestBean bean = new AuthenticatedRequestBean();
        bean.setTzOffset(-1);
        browserValuesProvider.checkTzDiscrepancy(bean, null, this.date);

        bean.setTzOffset(NY_DST_TZ_OFFSET);
        bean.setTzFromBrowser(NY_TZ_ID);
        browserValuesProvider.checkTzDiscrepancy(bean, NY_TZ, this.date);
    }

    @Test
    public void testCheckTzDiscrepancyNullTz() throws Exception {
        AuthenticatedRequestBean bean = new AuthenticatedRequestBean();
        bean.setTzOffset(NY_DST_TZ_OFFSET);
        Assertions.assertThrows(BrowserValuesProvider.TzDiscrepancyException.class, () -> {
            browserValuesProvider.checkTzDiscrepancy(bean, null, this.date);
        });
    }

    @Test
    public void testCheckTzDiscrepancyFalseTz() throws Exception {
        AuthenticatedRequestBean bean = new AuthenticatedRequestBean();
        bean.setTzOffset(0);
        Assertions.assertThrows(BrowserValuesProvider.TzDiscrepancyException.class, () -> {
            browserValuesProvider.checkTzDiscrepancy(bean, NY_TZ, this.date);
        });
    }

    @Test
    public void testCheckTzDiscrepancyFalseNonsenseTz() throws Exception {
        AuthenticatedRequestBean bean = new AuthenticatedRequestBean();
        bean.setTzOffset(NY_DST_TZ_OFFSET);
        Assertions.assertThrows(BrowserValuesProvider.TzDiscrepancyException.class, () -> {
            browserValuesProvider.checkTzDiscrepancy(bean, TimeZone.getTimeZone("adaf"), this.date);
        });
    }

}
