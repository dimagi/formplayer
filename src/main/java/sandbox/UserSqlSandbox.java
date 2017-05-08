package sandbox;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.cases.model.StorageIndexedTreeElementModel;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.modern.database.DatabaseIndexingUtils;
import org.commcare.modern.database.IndexedFixturePathsConstants;
import org.commcare.modern.util.Pair;
import org.javarosa.core.model.User;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import services.ConnectionHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A sandbox for user data using SqliteIndexedStorageUtility. Sandbox is per-User
 *
 * @author wspride
 */
public class UserSqlSandbox extends UserSandbox implements ConnectionHandler {

    // Need a different key than the default "Case" which is reserved by SQL
    public final static String FORMPLAYER_CASE = "CCCase";

    private final SqliteIndexedStorageUtility<Case> caseStorage;
    private final SqliteIndexedStorageUtility<Ledger> ledgerStorage;
    private final SqliteIndexedStorageUtility<User> userStorage;
    private final SqliteIndexedStorageUtility<FormInstance> userFixtureStorage;
    private final SqliteIndexedStorageUtility<FormInstance> appFixtureStorage;
    private final SqliteIndexedStorageUtility<StorageIndexedTreeElementModel> sqlUtil;
    private User user = null;
    public static final String DEFAULT_DATBASE_PATH = "dbs";
    private ConnectionHandler handler;

    /**
     * Create a sandbox of the necessary storage objects with the shared
     * factory.
     */
    public UserSqlSandbox(ConnectionHandler handler) {
        this.handler = handler;
        //we can't name this table "Case" becase that's reserved by sqlite
        caseStorage = new SqliteIndexedStorageUtility<>(handler, Case.class, FORMPLAYER_CASE);
        ledgerStorage = new SqliteIndexedStorageUtility<>(handler, Ledger.class, Ledger.STORAGE_KEY);
        userStorage = new SqliteIndexedStorageUtility<>(handler, User.class, User.STORAGE_KEY);
        userFixtureStorage = new SqliteIndexedStorageUtility<>(handler, FormInstance.class, "UserFixture");
        appFixtureStorage = new SqliteIndexedStorageUtility<>(handler, FormInstance.class, "AppFixture");
        sqlUtil = createFixturePathsTable(IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_TABLE);
    }

    @Override
    public SqliteIndexedStorageUtility<Case> getCaseStorage() {
        return caseStorage;
    }

    @Override
    public SqliteIndexedStorageUtility<Ledger> getLedgerStorage() {
        return ledgerStorage;
    }

    @Override
    public SqliteIndexedStorageUtility<User> getUserStorage() {
        return userStorage;
    }

    @Override
    public IStorageUtilityIndexed<StorageIndexedTreeElementModel> getIndexedFixtureStorage(String fixtureName) {
        String tableName = StorageIndexedTreeElementModel.getTableName(fixtureName);
        return new SqliteIndexedStorageUtility<>(handler,
                StorageIndexedTreeElementModel.class,
                tableName,
                false);
    }

    @Override
    public void setupIndexedFixtureStorage(String fixtureName,
                                           StorageIndexedTreeElementModel exampleEntry,
                                           Set<String> indices) {
        String tableName = StorageIndexedTreeElementModel.getTableName(fixtureName);
        SqliteIndexedStorageUtility<StorageIndexedTreeElementModel> sqlUtil
                = new SqliteIndexedStorageUtility<>(handler, exampleEntry, tableName);
        sqlUtil.rebuildTable(exampleEntry);
        sqlUtil.executeStatements(DatabaseIndexingUtils.getIndexStatements(tableName, indices));
    }

    @Override
    public Pair<String, String> getIndexedFixturePathBases(String fixtureName) {
        Connection connection;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = sqlUtil.getConnection();
            preparedStatement =
                    connection.prepareStatement(IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_TABLE_SELECT_STMT);
            preparedStatement.setString(1, fixtureName);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String base = resultSet.getString(1);
                String child = resultSet.getString(2);
                return new Pair<>(base, child);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public void setIndexedFixturePathBases(String fixtureName, String baseName,
                                           String childName) {
        Map<String, String> contentVals = new HashMap<>();
        contentVals.put(IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_COL_BASE, baseName);
        contentVals.put(IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_COL_CHILD, childName);
        contentVals.put(IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_COL_NAME, fixtureName);
        sqlUtil.basicInsert(contentVals);
    }

    /**
     * create 'fixture paths' table and an index over that table
     */
    private SqliteIndexedStorageUtility<StorageIndexedTreeElementModel> createFixturePathsTable(String tableName) {
        // NOTE PLM: this should maybe be done on server startup instead on
        // ever invocation
        SqliteIndexedStorageUtility<StorageIndexedTreeElementModel> sqlUtil =
                new SqliteIndexedStorageUtility<>(handler, null, tableName, false);
        String[] indexTableStatements = new String[]{
                IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_TABLE_STMT,
                // NOTE PLM: commenting out index creation below because
                // it will crash if run multiple times. We should find a way to
                // establish the index.
                IndexedFixturePathsConstants.INDEXED_FIXTURE_INDEXING_STMT
        };
        sqlUtil.executeStatements(indexTableStatements);

        return sqlUtil;
    }

    @Override
    public SqliteIndexedStorageUtility<FormInstance> getUserFixtureStorage() {
        return userFixtureStorage;
    }

    @Override
    public SqliteIndexedStorageUtility<FormInstance> getAppFixtureStorage() {
        return appFixtureStorage;
    }

    @Override
    public User getLoggedInUser() {
        if (user == null) {
            SqliteIndexedStorageUtility<User> userStorage = getUserStorage();
            AbstractSqlIterator<User> iterator = userStorage.iterate();
            if (iterator.hasMore()) {
                // should be only one user here
                user = iterator.nextRecord();
            } else {
                user = null;
            }
        }
        return user;
    }

    @Override
    public void setLoggedInUser(User user) {
        this.user = user;
    }

    @Override
    public Connection getConnection() {
        return handler.getConnection();
    }
}
