package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Regression tests for fixed behaviors
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class IndexedFixtureTest extends BaseTestClass {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("indexeddomain", "indexedusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/indexed.xml";
    }

    @Test
    public void testBadCaseSelection() throws Throwable {
        sessionNavigate(new String[]{"0", "action 0"}, "indexed", null, NewFormResponse.class, "test");
        sessionNavigate(new String[]{"1", "d83cf96f-6f7d-40e1-b3eb-ee8b2b8fed0f", "0"}, "indexed", null, NewFormResponse.class, "test");
    }
}
