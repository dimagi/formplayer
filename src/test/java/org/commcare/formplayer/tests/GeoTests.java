package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Tests for geo functionality
 */
@WebMvcTest
public class GeoTests extends BaseTestClass {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("heredoman", "hereusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/geo.xml";
    }
}
