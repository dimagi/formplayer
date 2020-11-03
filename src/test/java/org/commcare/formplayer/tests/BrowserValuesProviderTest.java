package org.commcare.formplayer.tests;

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
        browserValuesProvider.checkTzDiscrepancy(null, -1, this.date);
        browserValuesProvider.checkTzDiscrepancy(TimeZone.getTimeZone("America/New_York"),
                -14400000, this.date);
    }

    @Test
    public void testCheckTzDiscrepancyNullTz() throws Exception {
        thrown.expect(BrowserValuesProvider.TzDiscrepancyException.class);
        browserValuesProvider.checkTzDiscrepancy(null, -14400000, this.date);
    }

    @Test
    public void testCheckTzDiscrepancyFalseTz() throws Exception {
        thrown.expect(BrowserValuesProvider.TzDiscrepancyException.class);
        browserValuesProvider.checkTzDiscrepancy(TimeZone.getTimeZone("America/New_York"), 0, this.date);
    }

    @Test
    public void testCheckTzDiscrepancyFalseNonsenseTz() throws Exception {
        thrown.expect(BrowserValuesProvider.TzDiscrepancyException.class);
        browserValuesProvider.checkTzDiscrepancy(TimeZone.getTimeZone("adaf"), -1, this.date);
    }

}
