package org.commcare.formplayer.tests;

import static org.commcare.formplayer.utils.DbTestUtils.evaluate;

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
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;


@WebMvcTest
@Import({UtilController.class})
@ContextConfiguration(classes = {TestContext.class, CacheConfiguration.class})
@ExtendWith(InitializeStaticsExtension.class)
public class CaseDbModelQueryTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestoreFactory restoreFactory;

    @RegisterExtension
    static RestoreFactoryExtension restoreFactoryExt = new RestoreFactoryExtension.builder()
            .withUser("synctestusername").withDomain("synctestdomain")
            .withRestorePath("restores/dbtests/case_db_model_query.xml")
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

    @Test
    public void testModelQueryLookupDerivations() throws Exception {
        evaluate(
                "join(',',instance('casedb')/casedb/case[@case_type='unit_test_child_child"
                        + "'][@status='open'][true() and "
                        +
                        "instance('casedb')/casedb/case[@case_id = instance('casedb')"
                        + "/casedb/case[@case_id=current()/index/parent]/index/parent]/test = "
                        + "'true']/@case_id)",
                "child_ptwo_one_one,child_one_one", evaluationContext);
    }

    @Test
    public void testModelSelfReference() throws XPathSyntaxException {
        evaluate(
                "join(',',instance('casedb')/casedb/case[@case_type='unit_test_child'][@status"
                        + "='open'][true() and "
                        +
                        "count(instance('casedb')/casedb/case[index/parent = instance('casedb')"
                        + "/casedb/case[@case_id=current()/@case_id]/index/parent][false = "
                        + "'true']) > 0]/@case_id)",
                "", evaluationContext);

    }
}
