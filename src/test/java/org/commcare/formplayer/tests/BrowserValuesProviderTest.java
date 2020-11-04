package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.commcare.formplayer.services.BrowserValuesProvider;
import org.commcare.formplayer.utils.TestContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.LocalDate;
import java.util.Date;
import java.util.TimeZone;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class BrowserValuesProviderTest {

    static final String NY_TZ_ID = "America/New_York";
    static final TimeZone NY_TZ = TimeZone.getTimeZone(NY_TZ_ID);
    static final int NY_DST_TZ_OFFSET = -14400000;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    BrowserValuesProvider browserValuesProvider = null;
    Date date = null;

    @Before
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
        thrown.expect(BrowserValuesProvider.TzDiscrepancyException.class);
        AuthenticatedRequestBean bean = new AuthenticatedRequestBean();
        bean.setTzOffset(NY_DST_TZ_OFFSET);
        browserValuesProvider.checkTzDiscrepancy(bean, null, this.date);
    }

    @Test
    public void testCheckTzDiscrepancyFalseTz() throws Exception {
        thrown.expect(BrowserValuesProvider.TzDiscrepancyException.class);
        AuthenticatedRequestBean bean = new AuthenticatedRequestBean();
        bean.setTzOffset(0);
        browserValuesProvider.checkTzDiscrepancy(bean, NY_TZ, this.date);
    }

    @Test
    public void testCheckTzDiscrepancyFalseNonsenseTz() throws Exception {
        thrown.expect(BrowserValuesProvider.TzDiscrepancyException.class);
        AuthenticatedRequestBean bean = new AuthenticatedRequestBean();
        bean.setTzOffset(NY_DST_TZ_OFFSET);
        browserValuesProvider.checkTzDiscrepancy(bean, TimeZone.getTimeZone("adaf"), this.date);
    }

}
