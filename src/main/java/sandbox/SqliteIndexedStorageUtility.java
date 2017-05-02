package sandbox;

import org.commcare.modern.database.DatabaseHelper;
import org.commcare.modern.database.TableBuilder;
import org.commcare.modern.util.Pair;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.InvalidIndexException;
import org.javarosa.core.util.externalizable.DeserializationException;
import services.ConnectionHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * IStorageIndexedUtility implemented on SQLite using JDBC. Contains all the functionality
 * for interacting with the user's SQLite representation.
 *
 * @author wspride
 */
public class SqliteIndexedStorageUtility<T extends Persistable>
        implements IStorageUtilityIndexed<T>, Iterable<T> {

    private Class<T> prototype;
    private final String tableName;

    private ConnectionHandler connectionHandler;

    public SqliteIndexedStorageUtility(ConnectionHandler connectionHandler, T prototype, String tableName) {
        this(connectionHandler, (Class<T>) prototype.getClass(), tableName);
    }

    public SqliteIndexedStorageUtility(ConnectionHandler connectionHandler, Class<T> prototype, String tableName) {
        this(connectionHandler, prototype, tableName, true);
    }

    public SqliteIndexedStorageUtility(ConnectionHandler connectionHandler, Class<T> prototype,
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
        this.prototype = (Class<T>) prototypeInstance.getClass();

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
                c.prepareStatement(statement).execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void basicInsert(Map<String, String> contentVals) {
        Connection connection = getConnection();
        SqlHelper.basicInsert(connection, tableName, contentVals);
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
        Connection connection;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = this.getConnection();
            preparedStatement = SqlHelper.prepareTableSelectStatement(connection, this.tableName,
                    new String[]{fieldName}, new String[]{(String) value});
            if (preparedStatement == null) {
                return null;
            }
            resultSet = preparedStatement.executeQuery();
            return fillIdWindow(resultSet, DatabaseHelper.ID_COL, new LinkedHashSet<Integer>());
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
    }

    @Override
    public T getRecordForValue(String fieldName, Object value)
            throws NoSuchElementException, InvalidIndexException {
        Connection connection;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet;
        try {
            connection = this.getConnection();
            preparedStatement =
                    SqlHelper.prepareTableSelectStatement(connection, this.tableName,
                            new String[]{fieldName}, new String[]{(String) value});
            resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) {
                throw new NoSuchElementException();
            }
            byte[] mBytes = resultSet.getBytes(DatabaseHelper.DATA_COL);
            return newObject(mBytes, resultSet.getInt(DatabaseHelper.ID_COL));
        } catch (SQLException | NullPointerException e) {
            throw new RuntimeException(e);
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
        PreparedStatement preparedStatement = null;
        Connection connection;
        ResultSet resultSet = null;
        try {
            connection = getConnection();
            preparedStatement = SqlHelper.prepareIdSelectStatement(connection, this.tableName, id);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        } catch (Exception e) {
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
    public AbstractSqlIterator<T> iterate() {
        return iterate(true);
    }

    public AbstractSqlIterator<T> iterate(boolean includeData) {
        Connection connection;
        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = this.getConnection();
            String sqlQuery;
            if (includeData) {
                sqlQuery = "SELECT " + org.commcare.modern.database.DatabaseHelper.ID_COL + " , " +
                    org.commcare.modern.database.DatabaseHelper.DATA_COL + " FROM " + this.tableName + ";";
            } else {
                sqlQuery = "SELECT " + org.commcare.modern.database.DatabaseHelper.ID_COL +" FROM " + this.tableName + ";";
            }
            preparedStatement = connection.prepareStatement(sqlQuery);
            resultSet = preparedStatement.executeQuery();

            ArrayList<T> backingList = new ArrayList<>();
            ArrayList<Integer> idSet = new ArrayList<>();
            while (resultSet.next()) {
                if (includeData) {
                    T t = newObject(resultSet.getBytes(DatabaseHelper.DATA_COL), resultSet.getInt(DatabaseHelper.ID_COL));
                    backingList.add(t);
                } else {
                    idSet.add(resultSet.getInt(DatabaseHelper.ID_COL));
                }
            }
            if (includeData) {
                return new JdbcSqlStorageIterator<>(backingList);
            } else {
                return new JdbcSqlIndexIterator(idSet);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
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
    public boolean isEmpty() {
        return this.getNumRecords() <= 0;
    }

    @Override
    public byte[] readBytes(int id) {
        PreparedStatement preparedStatement = null;
        Connection connection;
        ResultSet resultSet = null;
        try {
            connection = getConnection();
            preparedStatement = SqlHelper.prepareIdSelectStatement(connection, this.tableName, id);
            if (preparedStatement == null) {
                return null;
            }
            resultSet = preparedStatement.executeQuery();
            return resultSet.getBytes(org.commcare.modern.database.DatabaseHelper.DATA_COL);
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
            try {
                if (resultSet != null) {
                    resultSet.close();
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

    // not yet implemented
    @Override
    public Vector<Integer> removeAll(EntityFilter ef) {
        Vector<Integer> removed = new Vector<>();
        for (IStorageIterator iterator = this.iterate(); iterator.hasMore(); ) {
            int id = iterator.nextID();
            switch (ef.preFilter(id, null)) {
                case EntityFilter.PREFILTER_INCLUDE:
                    removed.add(id);
                    continue;
                case EntityFilter.PREFILTER_EXCLUDE:
                    continue;
                case EntityFilter.PREFILTER_FILTER:
                    if (ef.matches(read(id))) {
                        removed.add(id);
                    }
            }
        }

        if (removed.size() == 0) {
            return removed;
        }

        List<Pair<String, String[]>> whereParamList = TableBuilder.sqlList(removed);

        for (Pair<String, String[]> whereParams : whereParamList) {
            SqlHelper.deleteFromTableWhere(getConnection(), tableName,
                    DatabaseHelper.ID_COL + " IN " + whereParams.first, whereParams.second);
        }

        return removed;
    }

    @Override
    public Iterator<T> iterator() {
        return (Iterator<T>) iterate();
    }

    public void getIDsForValues(String[] namesToMatch, String[] valuesToMatch, LinkedHashSet<Integer> ids) {
        Connection connection;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = this.getConnection();
            preparedStatement = SqlHelper.prepareTableSelectStatement(connection, this.tableName,
                    namesToMatch, valuesToMatch);
            if (preparedStatement == null) {
                return;
            }
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                ids.add(resultSet.getInt(DatabaseHelper.ID_COL));
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

    public void bulkRead(LinkedHashSet<Integer> body, HashMap<Integer, T> recordMap) {
        List<Pair<String, String[]>> whereParamList = TableBuilder.sqlList(body);
        PreparedStatement preparedStatement = null;
        Connection connection;
        ResultSet resultSet = null;
        try {
            connection = getConnection();
            for (Pair<String, String[]> querySet : whereParamList) {

                preparedStatement =
                        SqlHelper.prepareTableSelectStatement(connection,
                                this.tableName,
                                DatabaseHelper.ID_COL + " IN " + querySet.first,
                                querySet.second);
                resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    int index = resultSet.findColumn(DatabaseHelper.DATA_COL);
                    byte[] data = resultSet.getBytes(index);
                    recordMap.put(resultSet.getInt(DatabaseHelper.ID_COL),
                            newObject(data, resultSet.getInt(DatabaseHelper.ID_COL)));
                }
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
                PreparedStatement selectStatement = SqlHelper.prepareTableSelectStatement(connectionHandler.getConnection(),
                        tableName,
                        fieldName + " IN " + querySet.first,
                        querySet.second);
                ResultSet resultSet = selectStatement.executeQuery();
                while (resultSet.next()) {
                    byte[] data = resultSet.getBytes(DatabaseHelper.DATA_COL);
                    returnSet.add(newObject(data, resultSet.getInt(DatabaseHelper.ID_COL)));
                }
            }
            return returnSet;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
