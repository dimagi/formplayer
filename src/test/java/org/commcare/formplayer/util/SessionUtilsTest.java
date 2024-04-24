package org.commcare.formplayer.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SessionUtilsTest {

    @Test
    void resolveInstallReference() {
        String installReference = SessionUtils.resolveInstallReference(
                "test_app_id",
                "http://localhost:8000",
                "test_domain");
        String expectedInstallRef = "http://localhost:8000/a/test_domain/apps/api/download_ccz/?app_id=test_app_id&latest=save";
        assertEquals(installReference, expectedInstallRef);
    }
}
