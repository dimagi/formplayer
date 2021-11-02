package org.commcare.formplayer.tests;

import org.commcare.formplayer.auth.DjangoAuth;
import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.utils.TestContext;

/**
 * Created by benrudolph on 1/19/17.
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class RestoreFactoryTest {

    private String username = "restore-dude";
    private String domain = "restore-domain";
    private String asUsername = "restore-gal";

    @Autowired
    RestoreFactory restoreFactorySpy;

    @BeforeEach
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

        Assertions.assertTrue(restoreFactorySpy.isRestoreXmlExpired());
    }

    @Test
    public void testNotIsRestoreXmlExpiredDaily() {
        mockSyncFreq(RestoreFactory.FREQ_DAILY);

        // Last sync time should be less than a day ago
        mockLastSyncTime(System.currentTimeMillis());
        Assertions.assertFalse(restoreFactorySpy.isRestoreXmlExpired());
    }

    @Test
    public void testNullIsRestoreXmlExpired() {
        mockSyncFreq("default-case");

        mockLastSyncTime(null);
        Assertions.assertFalse(restoreFactorySpy.isRestoreXmlExpired());
    }

    @Test
    public void testIsRestoreXmlExpiredWeekly() {
        mockSyncFreq(RestoreFactory.FREQ_WEEKLY);

        // Last sync time should be more than a week ago
        mockLastSyncTime(System.currentTimeMillis() - RestoreFactory.ONE_WEEK_IN_MILLISECONDS - 1);

        Assertions.assertTrue(restoreFactorySpy.isRestoreXmlExpired());
    }

    @Test
    public void testNotIsRestoreXmlExpiredWeekly() {
        mockSyncFreq(RestoreFactory.FREQ_WEEKLY);

        // Last sync time should be less than a week ago
        mockLastSyncTime(System.currentTimeMillis());

        Assertions.assertFalse(restoreFactorySpy.isRestoreXmlExpired());
    }
}
