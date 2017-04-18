package tests;

import auth.DjangoAuth;
import beans.AuthenticatedRequestBean;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import services.RestoreFactory;
import utils.TestContext;

/**
 * Created by benrudolph on 1/19/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class RestoreFactoryTest {

    private String username = "restore-dude";
    private String domain = "restore-domain";
    private String asUsername = "restore-gal";

    @Autowired
    RestoreFactory restoreFactorySpy;

    @Before
    public void setUp() throws Exception {
        Mockito.reset(restoreFactorySpy);
        MockitoAnnotations.initMocks(this);
        AuthenticatedRequestBean requestBean = new AuthenticatedRequestBean();
        requestBean.setRestoreAs(asUsername);
        requestBean.setUsername(username);
        requestBean.setDomain(domain);
        restoreFactorySpy.configure(requestBean, new DjangoAuth("key"));
    }

    private void mockSyncFreq(String freq) {
        Mockito.doReturn(freq)
                .when(restoreFactorySpy)
                .getSyncFreqency();
    }

    private void mockLastSyncTime(Long epoch) {
        Mockito.doReturn(epoch)
                .when(restoreFactorySpy)
                .getLastSyncTime();
    }

    @Test
    public void testIsRestoreXmlExpiredDaily() {
        mockSyncFreq(RestoreFactory.FREQ_DAILY);

        // Last sync time should be more than a day ago
        mockLastSyncTime(System.currentTimeMillis() - RestoreFactory.ONE_DAY_IN_MILLISECONDS - 1);

        Assert.assertTrue(restoreFactorySpy.isRestoreXmlExpired());
    }

    @Test
    public void testNotIsRestoreXmlExpiredDaily() {
        mockSyncFreq(RestoreFactory.FREQ_DAILY);

        // Last sync time should be less than a day ago
        mockLastSyncTime(System.currentTimeMillis());
        Assert.assertFalse(restoreFactorySpy.isRestoreXmlExpired());
    }

    @Test
    public void testNullIsRestoreXmlExpired() {
        mockSyncFreq("default-case");

        mockLastSyncTime(null);
        Assert.assertFalse(restoreFactorySpy.isRestoreXmlExpired());
    }

    @Test
    public void testIsRestoreXmlExpiredWeekly() {
        mockSyncFreq(RestoreFactory.FREQ_WEEKLY);

        // Last sync time should be more than a week ago
        mockLastSyncTime(System.currentTimeMillis() - RestoreFactory.ONE_WEEK_IN_MILLISECONDS - 1);

        Assert.assertTrue(restoreFactorySpy.isRestoreXmlExpired());
    }

    @Test
    public void testNotIsRestoreXmlExpiredWeekly() {
        mockSyncFreq(RestoreFactory.FREQ_WEEKLY);

        // Last sync time should be less than a week ago
        mockLastSyncTime(System.currentTimeMillis());

        Assert.assertFalse(restoreFactorySpy.isRestoreXmlExpired());
    }
}
