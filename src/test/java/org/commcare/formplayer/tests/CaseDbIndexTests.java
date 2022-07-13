package org.commcare.formplayer.tests;

import static org.commcare.formplayer.utils.DbTestUtils.evaluate;

import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.utils.TestContext;
import org.commcare.formplayer.utils.TestStorageUtils;
import org.javarosa.core.model.condition.EvaluationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author wspride
 */
@WebMvcTest
public class CaseDbIndexTests extends BaseTestClass {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("synctestdomain", "synctestusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/dbtests/case_create_index.xml";
    }

    /**
     * Tests for basic common case database queries
     */
    @Test
    public void testCaseCreateIndex() throws Exception {
        syncDb();
        UserSqlSandbox sandbox = getRestoreSandbox();
        EvaluationContext ec = TestStorageUtils.getEvaluationContextWithoutSession(sandbox);
        evaluate("instance('casedb')/casedb/case[@case_id = 'test_case_id_child']/index/parent",
                "test_case_id", ec);
        evaluate("instance('casedb')/casedb/case[@case_id = 'test_case_id']/index/missing", "", ec);
        evaluate(
                "instance('casedb')/casedb/case[@case_type = 'unit_test_child'][index/parent = "
                        + "'test_case_id_2']/@case_id",
                "test_case_id_child_2", ec);
        evaluate(
                "count(instance('casedb')/casedb/case[@case_type = "
                        + "'unit_test_child'][index/parent != 'test_case_id_2'])",
                "1", ec);
    }
}
