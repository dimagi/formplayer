package org.commcare.formplayer.tests;

import com.google.common.collect.ImmutableMap;
import org.commcare.cases.query.QueryContext;
import org.commcare.cases.query.queryset.CurrentModelQuerySet;
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
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.trace.AccumulatingReporter;
import org.javarosa.core.model.trace.EvaluationTraceReporter;
import org.javarosa.core.model.utils.InstrumentationUtils;
import org.javarosa.xpath.XPathLazyNodeset;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.function.Predicate;

import static org.commcare.formplayer.utils.DbTestUtils.evaluate;


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

    @Test
    public void testModelReverseIndexLookup() throws XPathSyntaxException {
        XPathLazyNodeset nodeset = (XPathLazyNodeset) XPathParseTool.parseXPath(
                "instance('casedb')/casedb/case[@case_type='unit_test_parent']").eval(evaluationContext);

        ImmutableMap<String, String> expectedOutputs = ImmutableMap.of(
                "test_case_parent", "child_one,child_two,child_three",
                "parent_two", "child_ptwo_one"
        );
        Assertions.assertEquals(nodeset.getReferences().size(), expectedOutputs.size());

        EvaluationTraceReporter reporter = new AccumulatingReporter();
        evaluationContext.setDebugModeOn(reporter);
        for (TreeReference current : nodeset.getReferences()) {
            EvaluationContext subContext = new EvaluationContext(evaluationContext, current);
            QueryContext newContext = subContext.getCurrentQueryContext()
                    .checkForDerivativeContextAndReturn(nodeset.getReferences().size());
            newContext.setHackyOriginalContextBody(new CurrentModelQuerySet(nodeset.getReferences()));
            subContext.setQueryContext(newContext);

            String parentCaseId = FunctionUtils.toString(
                 XPathParseTool.parseXPath("./@case_id").eval(subContext)
            );

            evaluate(
                    "join(',',instance('casedb')/casedb/case[index/parent = current()/@case_id]/@case_id)",
                    expectedOutputs.get(parentCaseId),
                    subContext
            );

        }
        Predicate<String> predicate = line -> line.contains("Load Query Set Transform[current]=>[current|reverse index|parent]: Loaded: 4");
        int matchedReverseIndexLoad = InstrumentationUtils.countMatchedTraces(reporter, predicate);
        Assertions.assertEquals(1, matchedReverseIndexLoad);

        int matchedLookups = InstrumentationUtils.countMatchedTraces(reporter, line -> line.contains("QuerySetLookup|current|reverse index|parent: Results:"));
        Assertions.assertEquals(2, matchedLookups);
    }

    @Test
    public void testModelIndexLookup() throws XPathSyntaxException {
        XPathLazyNodeset nodeset = (XPathLazyNodeset) XPathParseTool.parseXPath(
                "instance('casedb')/casedb/case[@case_type='unit_test_child']").eval(evaluationContext);
        EvaluationTraceReporter reporter = new AccumulatingReporter();
        evaluationContext.setDebugModeOn(reporter);

        ImmutableMap<String, String> expectedOutputs = ImmutableMap.of(
                "child_one", "test_case_parent",
                "child_two", "test_case_parent",
                "child_three", "test_case_parent",
                "child_ptwo_one", "parent_two"
        );

        Assertions.assertEquals(nodeset.getReferences().size(), expectedOutputs.size());

        for (TreeReference current : nodeset.getReferences()) {
            EvaluationContext subContext = new EvaluationContext(evaluationContext, current);
            QueryContext newContext = subContext.getCurrentQueryContext()
                    .checkForDerivativeContextAndReturn(nodeset.getReferences().size());
            newContext.setHackyOriginalContextBody(new CurrentModelQuerySet(nodeset.getReferences()));
            subContext.setQueryContext(newContext);

            String childCaseId = FunctionUtils.toString(
                    XPathParseTool.parseXPath("./@case_id").eval(subContext)
            );

            evaluate(
                    "join(',',instance('casedb')/casedb/case[@case_id = current()/index/parent]/@case_id)",
                    expectedOutputs.get(childCaseId),
                    subContext
            );

        }
        Predicate<String> predicate = line -> line.contains("Load Query Set Transform[current]=>[current|index|parent]: Loaded: 2");
        int matchedReverseIndexLoad = InstrumentationUtils.countMatchedTraces(reporter, predicate);
        Assertions.assertEquals(1, matchedReverseIndexLoad);

        int matchedLookups = InstrumentationUtils.countMatchedTraces(reporter, line -> line.contains("QuerySetLookup|current|index|parent: Results: 1"));
        Assertions.assertEquals(4, matchedLookups);
    }
}
