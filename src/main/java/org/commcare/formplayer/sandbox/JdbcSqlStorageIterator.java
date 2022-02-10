
package org.commcare.formplayer.sandbox;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.modern.database.DatabaseHelper;
import org.commcare.modern.database.TableBuilder;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.ArrayUtilities;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Iterator over JDBC ResultSet
 *
 * @author ctsims
 * @author wspride
 */
public class JdbcSqlStorageIterator<T extends Persistable> implements IStorageIterator<T>, Iterator<T> {

    protected PreparedStatement preparedStatement;
    protected ResultSet resultSet;
    private final Set<String> metaDataIndexSet;
    SqlStorage<T> storage;
    private HashMap<String, Integer> metaDataColumnMap = new HashMap<>();

    private static final Log log = LogFactory.getLog(JdbcSqlStorageIterator.class);

    public JdbcSqlStorageIterator(PreparedStatement preparedStatement,
                                  ResultSet resultSet,
                                  SqlStorage<T> storage,
                                  String[] metaDataIndexSet) {
        this.resultSet = resultSet;
        this.storage = storage;
        this.metaDataIndexSet = new HashSet<>(ArrayUtilities.toVector(metaDataIndexSet));
        this.preparedStatement = preparedStatement;
        try {
            if (!resultSet.next()) {
                close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int peekID() {
        try {
            return resultSet.getInt(DatabaseHelper.ID_COL);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T nextRecord() {
        try {
            byte[] data = resultSet.getBytes(DatabaseHelper.DATA_COL);
            return storage.newObject(data, nextID());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int numRecords() {
        throw new RuntimeException("NumRecords not implemented for JdbcSqlStorageIterator");
    }

    @Override
    public int nextID() {
        try {
            int id = resultSet.getInt(DatabaseHelper.ID_COL);
            resultSet.next();
            if (resultSet.isAfterLast()) {
                close();
            }
            return id;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the indexed metadata for the current record _without_ advancing
     * the iterator, so this needs to be called _before_ next() or nextId()
     *
     * @param metadataKey The metadata key for this Serializable record. Would be
     *                    the same key used with T.getMetaData()
     * @throws RuntimeException If this iterator was not intialized with metadata
     */
    public String peekIncludedMetadata(String metadataKey) {
        try {
            if (!metaDataIndexSet.contains(metadataKey)) {
                throw new RuntimeException("Invalid iterator metadata request for key: " + metadataKey);
            }
            int columnIndex;
            if (metaDataColumnMap.containsKey(metadataKey)) {
                columnIndex = metaDataColumnMap.get(metadataKey);
            } else {
                String columnName = TableBuilder.scrubName(metadataKey);
                columnIndex = resultSet.findColumn(columnName);
                metaDataColumnMap.put(metadataKey, columnIndex);
            }
            return resultSet.getString(columnIndex);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasMore() {
        try {
            if (!resultSet.isClosed()) {
                if (resultSet.isAfterLast()) {
                    close();
                    return false;
                }
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {
        return hasMore();
    }

    @Override
    public T next() {
        return nextRecord();
    }

    @Override
    public void remove() {
        throw new RuntimeException("Tried to use undefined remove() function in JdbcSqlStorageIterator");
    }

    public void close() {
        try {
            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            log.error("Unable to close JdbcSqlStorageIterator resources", e);
        }
    }
}
