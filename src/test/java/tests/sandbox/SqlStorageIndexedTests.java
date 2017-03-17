package tests.sandbox;

import junit.framework.TestCase;
import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sandbox.JdbcSqlStorageIterator;
import sandbox.SqlSandboxUtils;
import sandbox.SqliteIndexedStorageUtility;
import sandbox.UserSqlSandbox;

import javax.sql.DataSource;
import java.util.Vector;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SqlStorageIndexedTests {

    private Case a, b, c;

    private Ledger l, l2, l3;

    private SqliteIndexedStorageUtility<Case> caseStorage;
    private SqliteIndexedStorageUtility<Ledger> ledgerStorage;
    private String owner;
    private String groupOwner;
    private String otherOwner;
    private Vector<String> groupOwned;
    private Vector<String> userOwned;


    @Before
    public void setUp() throws Exception {

        owner = "owner";
        otherOwner = "otherowner";
        groupOwner = "groupowned";

        userOwned = new Vector<>();
        userOwned.addElement(owner);

        groupOwned = new Vector<>();
        groupOwned.addElement(owner);
        groupOwned.addElement(groupOwner);

        a = new Case("a_case_name", "case_type_ipsum");
        a.setCaseId("a_case_id");
        a.setUserId(owner);
        a.setID(-1);

        b = new Case("b_case_name", "case_type_ipsum");
        b.setCaseId("b_case_id");
        b.setUserId(owner);
        b.setID(-1);

        c = new Case("c_case_name", "case_type_ipsum");
        c.setCaseId("c_case_id");
        c.setUserId(owner);
        c.setID(-1);

        l = new Ledger("ledger_entity_id");
        l.setID(-1);
        l.setEntry("test_section_id", "test_entry_id", 2345);

        l2 = new Ledger("ledger_entity_id_2");
        l2.setID(-1);
        l2.setEntry("test_section_id_2", "test_entry_id_2", 2345);

        l3 = new Ledger("ledger_entity_id_3");
        l3.setID(-1);
        l3.setEntry("test_section_id_3", "test_entry_id_3", 2345);
    }

    @Test
    public void testSqlCaseStorage() {

        PrototypeFactory mPrototypeFactory = new PrototypeFactory();
        mPrototypeFactory.addClass(Case.class);

        String storageKey = "TFCase";
        String username = "sql-storage-test";

        UserSqlSandbox sandbox = new UserSqlSandbox(new TestConnectionHandler(UserSqlSandbox.DEFAULT_DATBASE_PATH));
        caseStorage = new SqliteIndexedStorageUtility<>(sandbox, Case.class, storageKey);

        caseStorage.write(a);

        Case readCase = caseStorage.read(1);
        assertEquals("a_case_name", readCase.getName());
        assertEquals(1, readCase.getID());
        Assert.assertEquals(1, caseStorage.getNumRecords());

        int bID = caseStorage.add(b);
        readCase = caseStorage.read(bID);
        assertEquals(bID, readCase.getID());
        assertEquals("b_case_id", readCase.getCaseId());
        Assert.assertEquals(2, caseStorage.getNumRecords());

        int id = caseStorage.add(c);
        Assert.assertEquals(3, caseStorage.getNumRecords());
        readCase = caseStorage.getRecordForValue("case-id", "c_case_id");
        assertEquals(id, readCase.getID());
        Assert.assertEquals(caseStorage.getIDsForValue("case-type", "case_type_ipsum").size(), 3);

        caseStorage.remove(1);

        Assert.assertEquals(2, caseStorage.getNumRecords());
        try {
            caseStorage.read(1);
            org.junit.Assert.fail();
        } catch (NullPointerException e) {
            //good
        }

        for (Case mCase : caseStorage) {
            String caseId = mCase.getCaseId();
            assertTrue(caseId.equals("b_case_id") || caseId.equals("c_case_id"));
        }

        caseStorage.removeAll();

        Assert.assertEquals(0, caseStorage.getNumRecords());
    }

    @Test
    public void testSqlLedgerStorage() {
        try {

            PrototypeFactory mPrototypeFactory = new PrototypeFactory();
            mPrototypeFactory.addClass(Ledger.class);

            String storageKey = "Ledger";
            String username = "wspride";

            UserSqlSandbox sandbox = new UserSqlSandbox(new TestConnectionHandler(UserSqlSandbox.DEFAULT_DATBASE_PATH));
            ledgerStorage = new SqliteIndexedStorageUtility<>(sandbox, Ledger.class, storageKey);

            ledgerStorage.write(l);
            ledgerStorage.write(l2);
            ledgerStorage.write(l3);

            Vector ids = ledgerStorage.getIDsForValue("entity_id", "ledger_entity_id");

            assertEquals(1, ids.size());
            assertTrue(ids.contains(1));
            //assertTrue(ids.contains(2));

            Ledger readLedger2 = ledgerStorage.getRecordForValue("entity_id", "ledger_entity_id_3");
            assertEquals(readLedger2.getID(), 3);

            int count = ledgerStorage.getNumRecords();

            assertEquals(count, 3);

            assertTrue(ledgerStorage.exists(1));
            TestCase.assertFalse(ledgerStorage.exists(-123));

            IStorageIterator<Ledger> mIterator = ledgerStorage.iterate();

            Assert.assertEquals(3, mIterator.numRecords());
            Assert.assertEquals(1, mIterator.nextID());
            Assert.assertEquals(2, mIterator.nextID());
            Assert.assertEquals(3, mIterator.nextID());
            Assert.assertEquals(-1, mIterator.nextID());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }

    @After
    public void tearDown() {
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);
    }

}
