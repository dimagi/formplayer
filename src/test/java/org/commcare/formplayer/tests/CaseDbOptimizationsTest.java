package org.commcare.formplayer.tests;

import org.commcare.formplayer.application.UtilController;
import org.commcare.formplayer.configuration.CacheConfiguration;
import org.commcare.formplayer.junit.InitializeStaticsExtension;
import org.commcare.formplayer.junit.RestoreFactoryExtension;
import org.commcare.formplayer.junit.StorageFactoryExtension;
import org.commcare.formplayer.junit.request.SyncDbRequest;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.utils.TestContext;
import org.commcare.formplayer.utils.TestStorageUtils;
import org.javarosa.core.model.condition.EvaluationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.commcare.formplayer.utils.DbTestUtils.evaluate;

@WebMvcTest
@Import({UtilController.class})
@ContextConfiguration(classes = {TestContext.class, CacheConfiguration.class})
@ExtendWith(InitializeStaticsExtension.class)
public class CaseDbOptimizationsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestoreFactory restoreFactory;

    @RegisterExtension
    static RestoreFactoryExtension restoreFactoryExt = new RestoreFactoryExtension.builder()
            .withUser("synctestusername").withDomain("synctestdomain")
            .withRestorePath("restores/dbtests/case_test_db_optimizations.xml")
            .build();

    @RegisterExtension
    static StorageFactoryExtension storageExt = new StorageFactoryExtension.builder()
            .withUser("back_nav").withDomain("back_nav").build();
    private UserSqlSandbox sandbox;
    private EvaluationContext evaluationContext;

    @BeforeEach
    public void setUp() {
        new SyncDbRequest(mockMvc, restoreFactory).request();
        sandbox = restoreFactory.getSqlSandbox();
        evaluationContext = TestStorageUtils.getEvaluationContextWithoutSession(sandbox);
        evaluationContext.setDebugModeOn();
    }
    /**
     * Tests for basic common case database queries
     */
    @Test
    public void testDbOptimizations() throws Exception {
        EvaluationContext ec = TestStorageUtils.getEvaluationContextWithoutSession(sandbox);
        ec.setDebugModeOn();
        evaluate(
                "sort(join(' ',instance('casedb')/casedb/case[index/parent = "
                        + "'test_case_parent']/@case_id))",
                "child_one child_three child_two", evaluationContext);
        evaluate(
                "join(' ',instance('casedb')/casedb/case[index/parent = "
                        + "'test_case_parent'][@case_id = 'child_two']/@case_id)",
                "child_two", evaluationContext);
        evaluate(
                "sort(join(' ',instance('casedb')/casedb/case[index/parent = "
                        + "'test_case_parent'][@case_id != 'child_two']/@case_id))",
                "child_one child_three", evaluationContext);
    }

    @Test
    public void testSelectedOptimization() throws Exception {
        EvaluationContext ec = TestStorageUtils.getEvaluationContextWithoutSession(sandbox);
        ec.setDebugModeOn();
        evaluate(
                "sort(join(' ',instance('casedb')/casedb/case[selected('test_case_parent', "
                        + "index/parent)]/@case_id))",
                "child_one child_three child_two", evaluationContext);
        evaluate(
                "sort(join(' ',instance('casedb')/casedb/case[selected('test_case_parent "
                        + "test_case_parent_2', index/parent)]/@case_id))",
                "child_one child_three child_two", evaluationContext);
        evaluate(
                "sort(join(' ',instance('casedb')/casedb/case[selected('test_case_parent_2 "
                        + "test_case_parent', index/parent)]/@case_id))",
                "child_one child_three child_two", evaluationContext);
        evaluate(
                "join(' ',instance('casedb')/casedb/case[selected('test_case_parent_2 "
                        + "test_case_parent_3', index/parent)]/@case_id)",
                "", evaluationContext);
        evaluate("join(' ',instance('casedb')/casedb/case[selected('', index/parent)]/@case_id)",
                "", evaluationContext);
    }
}
