package org.commcare.formplayer.sandbox;

import org.commcare.formplayer.exceptions.SQLiteRuntimeException;
import org.commcare.modern.database.DatabaseHelper;
import org.commcare.modern.database.TableBuilder;
import org.commcare.modern.util.Pair;
import org.javarosa.core.model.condition.RequestAbandonedException;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.InvalidIndexException;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.commcare.formplayer.services.ConnectionHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * IStorageIndexedUtility implemented on SQLite using JDBC. Contains all the functionality
 * for interacting with the user's SQLite representation.
 *
 * @author wspride
 */
public class SqlStorage<T extends Persistable>
        implements IStorageUtilityIndexed<T>, Iterable<T> {

    private Class<T> prototype;
    private final String tableName;

    private ConnectionHandler connectionHandler;

    public SqlStorage(ConnectionHandler connectionHandler, T prototype, String tableName) {
        this(connectionHandler, (Class<T>)prototype.getClass(), tableName);
    }

    public SqlStorage(ConnectionHandler connectionHandler, Class<T> prototype, String tableName) {
        this(connectionHandler, prototype, tableName, true);
    }

    public SqlStorage(ConnectionHandler connectionHandler, Class<T> prototype,
                      String tableName, boolean initialize) {
        this.tableName = tableName;
        this.prototype = prototype;
        this.connectionHandler = connectionHandler;
        if (initialize) {
            try {
                buildTableFromInstance(prototype.newInstance());
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void rebuildTable(T prototypeInstance) {
        this.prototype = (Class<T>)prototypeInstance.getClass();

        try {
            SqlHelper.dropTable(getConnection(), tableName);
            buildTableFromInstance(prototypeInstance);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void executeStatements(String[] statements) {
        Connection c;
        try {
            c = getConnection();
            for (String statement : statements) {
                try (PreparedStatement preparedStatement = c.prepareStatement(statement)) {
                    preparedStatement.execute();
                }
            }
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    public void basicInsert(Map<String, Object> contentVals) {
        Connection connection = getConnection();
        SqlHelper.basicInsert(connection, tableName, contentVals);
    }

    public void insertOrReplace(Map<String, Object> contentVals) {
        Connection connection = getConnection();
        SqlHelper.insertOrReplace(connection, tableName, contentVals);
    }

    private void buildTableFromInstance(T instance) throws ClassNotFoundException {
        Connection connection = getConnection();
        SqlHelper.createTable(connection, tableName, instance);
    }

    public Connection getConnection() {
        return connectionHandler.getConnection();
    }

    @Override
    public void write(Persistable p) {
        if (p.getID() != -1) {
            update(p.getID(), p);
            return;
        }
        Connection connection;
        connection = getConnection();
        int id = SqlHelper.insertToTable(connection, tableName, p);
        p.setID(id);
    }

    @Override
    public T read(int id) {
        return newObject(readBytes(id), id);
    }

    public static Vector<Integer> fillIdWindow(ResultSet resultSet, String columnName, LinkedHashSet newReturn) throws SQLException {
        Vector<Integer> ids = new Vector<>();
        while (resultSet.next()) {
            ids.add(resultSet.getInt(columnName));
            newReturn.add(resultSet.getInt(columnName));
        }
        return ids;
    }

    @Override
    public Vector<Integer> getIDsForValue(String fieldName, Object value) {
        Connection connection = this.getConnection();
        try (PreparedStatement preparedStatement = SqlHelper.prepareTableSelectStatement(connection,
                this.tableName, new String[]{fieldName}, new String[]{(String)value})) {

            if (preparedStatement == null) {
                return null;
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return fillIdWindow(resultSet, DatabaseHelper.ID_COL, new LinkedHashSet<Integer>());
            }
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    @Override
    public List<Integer> getIDsForValues(String[] fieldNames, Object[] values) {
        return getIDsForValues(fieldNames, values, null);
    }

    @Override
    public List<Integer> getIDsForValues(String[] fieldNames, Object[] values, LinkedHashSet<Integer> returnSet) {
        Connection connection = this.getConnection();
        try (PreparedStatement preparedStatement =
                     SqlHelper.prepareTableSelectStatement(connection, this.tableName, fieldNames, values)) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return fillIdWindow(resultSet, DatabaseHelper.ID_COL, returnSet);
            }
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    @Override
    public T getRecordForValue(String fieldName, Object value)
            throws NoSuchElementException, InvalidIndexException {
        return getRecordsForValues(new String[]{fieldName}, new Object[]{value}).get(0);
    }

    @Override
    public Vector<T> getRecordsForValues(String[] metaFieldNames, Object[] values) {
        Connection connection = this.getConnection();

        try (PreparedStatement preparedStatement =
                     SqlHelper.prepareTableSelectStatement(connection, this.tableName,
                             metaFieldNames, values)) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                Vector<T> resultObjects = new Vector<>();
                while (resultSet.next()) {
                    byte[] mBytes = resultSet.getBytes(DatabaseHelper.DATA_COL);
                    resultObjects.add(newObject(mBytes, resultSet.getInt(DatabaseHelper.ID_COL)));
                }
                if (resultObjects.size() == 0) {
                    throw new NoSuchElementException();
                }
                return resultObjects;
            }
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    @Override
    public int add(T e) {
        Connection connection = getConnection();
        int id = SqlHelper.insertToTable(connection, tableName, e);
        connection = getConnection();
        e.setID(id);
        SqlHelper.updateId(connection, tableName, e);
        return id;
    }

    @Override
    public void close() {
        // Don't need this because we close all resources after using them
    }

    @Override
    public boolean exists(int id) {
        Connection connection = getConnection();
        try (PreparedStatement preparedStatement =
                     SqlHelper.prepareIdSelectStatement(connection, this.tableName, id)) {

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return true;
                }
            }

        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
        return false;
    }

    @Override
    public Object getAccessLock() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getNumRecords() {
        PreparedStatement preparedStatement = null;
        Connection connection;
        ResultSet resultSet = null;
        try {
            connection = getConnection();
            String sqlQuery = "SELECT COUNT (*) FROM " + this.tableName + ";";
            preparedStatement = connection.prepareStatement(sqlQuery);
            resultSet = preparedStatement.executeQuery();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            // pass
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
        return -1;
    }

    @Override
    public JdbcSqlStorageIterator<T> iterate() {
        return iterate(true);
    }

    @Override
    public JdbcSqlStorageIterator<T> iterate(boolean includeData) {
        return iterate(includeData, new String[]{});
    }

    public JdbcSqlStorageIterator<T> iterate(boolean includeData, String[] metaDataToInclude) {
        try {
            String[] projection = getProjectedFieldsWithId(includeData, scrubMetadataNames(metaDataToInclude));
            PreparedStatement preparedStatement = SqlHelper.prepareTableSelectProjectionStatement(this.getConnection(), tableName, projection);
            ResultSet resultSet = preparedStatement.executeQuery();
            return new JdbcSqlStorageIterator<>(preparedStatement, resultSet, this, metaDataToInclude);
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    private String[] scrubMetadataNames(String[] metaDataNames) {
        String[] scrubbedNames = new String[metaDataNames.length];

        for (int i = 0; i < metaDataNames.length; ++i) {
            scrubbedNames[i] = TableBuilder.scrubName(metaDataNames[i]);
        }
        return scrubbedNames;
    }

    private String[] getProjectedFieldsWithId(boolean includeData, String[] columnNamesToInclude) {
        String[] projection = new String[columnNamesToInclude.length + (includeData ? 2 : 1)];
        int firstIndex = 0;
        projection[firstIndex] = DatabaseHelper.ID_COL;
        firstIndex++;
        if (includeData) {
            projection[firstIndex] = DatabaseHelper.DATA_COL;
            firstIndex++;
        }
        for (int i = 0; i < columnNamesToInclude.length; ++i) {
            projection[i + firstIndex] = columnNamesToInclude[i];
        }
        return projection;
    }

    @Override
    public boolean isEmpty() {
        return this.getNumRecords() <= 0;
    }

    @Override
    public byte[] readBytes(int id) {
        PreparedStatement preparedStatement = null;
        Connection connection;
        try {
            connection = getConnection();
            preparedStatement = SqlHelper.prepareIdSelectStatement(connection, this.tableName, id);
            if (preparedStatement == null) {
                return null;
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.getBytes(org.commcare.modern.database.DatabaseHelper.DATA_COL);
            }
        } catch (SQLException e) {
            throw new NullPointerException("No result for id " + id);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void update(int id, Persistable p) {
        Connection connection = getConnection();
        SqlHelper.updateToTable(connection, tableName, p, id);
    }

    @Override
    public void remove(int id) {
        Connection connection = getConnection();
        SqlHelper.deleteIdFromTable(connection, tableName, id);
    }

    @Override
    public void remove(Persistable p) {
        this.remove(p.getID());
    }

    @Override
    public void removeAll() {
        Connection connection = getConnection();
        SqlHelper.deleteAllFromTable(connection, tableName);
    }


    public Vector<Integer> removeAll(Vector<Integer> toRemove) {
        if (toRemove.size() == 0) {
            return toRemove;
        }

        List<Pair<String, String[]>> whereParamList = TableBuilder.sqlList(toRemove);

        for (Pair<String, String[]> whereParams : whereParamList) {
            SqlHelper.deleteFromTableWhere(getConnection(), tableName,
                    DatabaseHelper.ID_COL + " IN " + whereParams.first, whereParams.second);
        }

        return toRemove;
    }

    // not yet implemented
    @Override
    public Vector<Integer> removeAll(EntityFilter ef) {
        Vector<Integer> toRemove = new Vector<>();
        for (IStorageIterator iterator = this.iterate(); iterator.hasMore(); ) {
            int id = iterator.nextID();
            switch (ef.preFilter(id, null)) {
                case EntityFilter.PREFILTER_INCLUDE:
                    toRemove.add(id);
                    continue;
                case EntityFilter.PREFILTER_EXCLUDE:
                    continue;
                case EntityFilter.PREFILTER_FILTER:
                    if (ef.matches(read(id))) {
                        toRemove.add(id);
                    }
            }
        }
        return removeAll(toRemove);
    }

    @Override
    public Iterator<T> iterator() {
        return iterate();
    }

    public void getIDsForValues(String[] namesToMatch, String[] valuesToMatch, LinkedHashSet<Integer> ids) {
        Connection connection = this.getConnection();

        try (PreparedStatement preparedStatement = SqlHelper.prepareTableSelectStatement(connection,
                this.tableName, namesToMatch, valuesToMatch)) {

            if (preparedStatement == null) {
                return;
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt(DatabaseHelper.ID_COL));
                }
            }
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    private RuntimeException logAndWrap(Exception e, String message) {
        RuntimeException re = new RuntimeException(message + " while inflating type " + prototype.getName());
        re.initCause(e);
        Logger.log("Error:", e.getMessage());
        return re;
    }

    /**
     * @param dbEntryId Set the deserialized persistable's id to the database entry id.
     *                  Doing so now is more effecient then during writes
     */
    public T newObject(InputStream serializedObjectInputStream, int dbEntryId) {
        try {
            T e = prototype.newInstance();
            e.readExternal(new DataInputStream(serializedObjectInputStream),
                    PrototypeManager.getDefault());
            e.setID(dbEntryId);
            return e;
        } catch (IllegalAccessException e) {
            throw logAndWrap(e, "Illegal Access Exception");
        } catch (InstantiationException e) {
            throw logAndWrap(e, "Instantiation Exception");
        } catch (IOException e) {
            throw logAndWrap(e, "Totally non-sensical IO Exception");
        } catch (DeserializationException e) {
            throw logAndWrap(e, "CommCare ran into an issue deserializing data");
        }
    }

    /**
     * @param dbEntryId Set the deserialized persistable's id to the database entry id.
     *                  Doing so now is more effecient then during writes
     */
    public T newObject(byte[] serializedObjectAsBytes, int dbEntryId) {
        return newObject(new ByteArrayInputStream(serializedObjectAsBytes), dbEntryId);
    }

    public void bulkRead(LinkedHashSet<Integer> cuedCases, HashMap<Integer, T> recordMap) throws RequestAbandonedException {
        List<Pair<String, String[]>> whereParamList = TableBuilder.sqlList(cuedCases);
        Connection connection = this.getConnection();
        try {
            for (Pair<String, String[]> querySet : whereParamList) {

                try (PreparedStatement preparedStatement =
                             SqlHelper.prepareTableSelectStatement(connection,
                                     this.tableName, DatabaseHelper.ID_COL + " IN " + querySet.first,
                                     querySet.second)) {
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        while (resultSet.next()) {
                            if (Thread.interrupted()) {
                                throw new RequestAbandonedException();
                            }
                            int index = resultSet.findColumn(DatabaseHelper.DATA_COL);
                            byte[] data = resultSet.getBytes(index);
                            recordMap.put(resultSet.getInt(DatabaseHelper.ID_COL),
                                    newObject(data, resultSet.getInt(DatabaseHelper.ID_COL)));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    @Override
    public String[] getMetaDataForRecord(int recordId, String[] metaFieldNames) {
        String recordIdString = String.valueOf(recordId);
        String[] scrubbedNames = scrubMetadataNames(metaFieldNames);
        String[] projection = getProjectedFieldsWithId(false, scrubbedNames);

        try (PreparedStatement preparedStatement = SqlHelper.prepareTableSelectProjectionStatement(
                getConnection(), tableName, recordIdString, projection
        )) {

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return readMetaDataFromResultSet(resultSet, scrubbedNames);
                } else {
                    throw new NoSuchElementException("No record in table " + tableName + " for ID " + recordId);
                }
            }
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    /**
     * Reads out the metadata columns from the provided cursor.
     *
     * NOTE: The column names _must be scrubbed here_ before the method is called
     */
    private String[] readMetaDataFromResultSet(ResultSet resultSet, String[] columnNames) throws SQLException {
        String[] results = new String[columnNames.length];
        int i = 0;
        for (String columnName : columnNames) {
            results[i] = resultSet.getString(columnName);
            i++;
        }
        return results;
    }

    @Override
    public void bulkReadMetadata(LinkedHashSet<Integer> recordIds, String[] metaFieldNames, HashMap<Integer, String[]> metadataMap) {
        List<Pair<String, String[]>> whereParamList = TableBuilder.sqlList(recordIds);
        String[] scrubbedNames = scrubMetadataNames(metaFieldNames);
        String[] projection = getProjectedFieldsWithId(false, scrubbedNames);
        Connection connection = getConnection();

        try {
            for (Pair<String, String[]> querySet : whereParamList) {

                try (PreparedStatement preparedStatement =
                             SqlHelper.prepareTableSelectStatementProjection(connection,
                                     this.tableName,
                                     DatabaseHelper.ID_COL + " IN " + querySet.first,
                                     querySet.second, projection)) {

                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        while (resultSet.next()) {
                            if (Thread.interrupted()) {
                                throw new RequestAbandonedException();
                            }
                            String[] metaRead = readMetaDataFromResultSet(resultSet, scrubbedNames);
                            metadataMap.put(resultSet.getInt(DatabaseHelper.ID_COL), metaRead);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }


    /**
     * Retrieves a set of the models in storage based on a list of values matching one if the
     * indexes of this storage
     */
    public List<T> getBulkRecordsForIndex(String indexName, Collection<String> matchingValues) {
        List<T> returnSet = new ArrayList<>();
        String fieldName = TableBuilder.scrubName(indexName);
        List<Pair<String, String[]>> whereParamList = TableBuilder.sqlList(matchingValues, "?");
        try {
            for (Pair<String, String[]> querySet : whereParamList) {
                try (PreparedStatement selectStatement = SqlHelper.prepareTableSelectStatement(connectionHandler.getConnection(),
                        tableName,
                        fieldName + " IN " + querySet.first,
                        querySet.second)) {

                    try (ResultSet resultSet = selectStatement.executeQuery()) {
                        while (resultSet.next()) {
                            byte[] data = resultSet.getBytes(DatabaseHelper.DATA_COL);
                            returnSet.add(newObject(data, resultSet.getInt(DatabaseHelper.ID_COL)));
                        }
                    }
                }
            }
            return returnSet;
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    public String getMetaDataFieldForRecord(int recordId, String rawFieldName) {
        String rid = String.valueOf(recordId);
        String scrubbedName = TableBuilder.scrubName(rawFieldName);

        try {
            try (PreparedStatement selectStatement = SqlHelper.prepareTableSelectStatement(
                    connectionHandler.getConnection(), tableName,
                    DatabaseHelper.ID_COL + "=?",
                    new String[]{rid})) {
                try (ResultSet resultSet = selectStatement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getString(scrubbedName);
                    } else {
                        throw new NoSuchElementException("No record in table " + tableName + " for ID " + recordId);
                    }
                }
            }
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }
}
