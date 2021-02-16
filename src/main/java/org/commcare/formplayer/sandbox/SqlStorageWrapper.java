package org.commcare.formplayer.sandbox;

import org.commcare.formplayer.services.ConnectionHandler;
import org.javarosa.core.model.condition.RequestAbandonedException;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.InvalidIndexException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * @author $|-|!˅@M
 */
class SqlStorageWrapper<T extends Persistable>
        implements IStorageUtilityIndexed<T>, Iterable<T> {

    SqlStorage<T> sqliteStorage;
    SqlStorage<T> postgresStorage;

    public SqlStorageWrapper(ConnectionHandler sqliteConnection, ConnectionHandler postgresConnection, Class<T> prototype, String tableName) {
        sqliteStorage = new SqlStorage<T>(sqliteConnection, prototype, tableName);
        postgresStorage = new SqlStorage<T>(postgresConnection, prototype, tableName);
    }


    public void rebuildTable(T prototypeInstance) {
        sqliteStorage.rebuildTable(prototypeInstance);
        postgresStorage.rebuildTable(prototypeInstance);
    }

    public void executeStatements(String[] statements) {
        sqliteStorage.executeStatements(statements);
        postgresStorage.executeStatements(statements);
    }

    public void basicInsert(Map<String, Object> contentVals) {
        sqliteStorage.basicInsert(contentVals);
        postgresStorage.basicInsert(contentVals);
    }

    public void insertOrReplace(Map<String, Object> contentVals) {
        sqliteStorage.insertOrReplace(contentVals);
        postgresStorage.insertOrReplace(contentVals);
    }

    @Override
    public void write(Persistable p) {
        sqliteStorage.write(p);
        postgresStorage.write(p);
    }

    @Override
    public T read(int id) {
        postgresStorage.read(id);
        return sqliteStorage.read(id);
    }

    @Override
    public Vector<Integer> getIDsForValue(String fieldName, Object value) {
        postgresStorage.getIDsForValue(fieldName, value);
        return sqliteStorage.getIDsForValue(fieldName, value);
    }

    @Override
    public List<Integer> getIDsForValues(String[] fieldNames, Object[] values) {
        postgresStorage.getIDsForValues(fieldNames, values);
        return sqliteStorage.getIDsForValues(fieldNames, values);
    }

    @Override
    public List<Integer> getIDsForValues(String[] fieldNames, Object[] values, LinkedHashSet<Integer> returnSet) {
        postgresStorage.getIDsForValues(fieldNames, values, returnSet);
        return sqliteStorage.getIDsForValues(fieldNames, values, returnSet);
    }

    @Override
    public T getRecordForValue(String fieldName, Object value)
            throws NoSuchElementException, InvalidIndexException {
        postgresStorage.getRecordForValue(fieldName, value);
        return sqliteStorage.getRecordForValue(fieldName, value);
    }

    @Override
    public Vector<T> getRecordsForValues(String[] metaFieldNames, Object[] values) {
        postgresStorage.getRecordsForValues(metaFieldNames, values);
        return sqliteStorage.getRecordsForValues(metaFieldNames, values);
    }

    @Override
    public int add(T e) {
        postgresStorage.add(e);
        return sqliteStorage.add(e);
    }

    @Override
    public void close() {
        // Don't need this because we close all resources after using them
    }

    @Override
    public boolean exists(int id) {
        postgresStorage.exists(id);
        return sqliteStorage.exists(id);
    }

    @Override
    public Object getAccessLock() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getNumRecords() {
        sqliteStorage.getNumRecords();
        return postgresStorage.getNumRecords();
    }

    @Override
    public JdbcSqlStorageIterator<T> iterate() {
        postgresStorage.iterate();
        return sqliteStorage.iterate();
    }

    @Override
    public JdbcSqlStorageIterator<T> iterate(boolean includeData) {
        postgresStorage.iterate(includeData);
        return sqliteStorage.iterate(includeData);
    }

    public JdbcSqlStorageIterator<T> iterate(boolean includeData, String[] metaDataToInclude) {
        postgresStorage.iterate(includeData, metaDataToInclude);
        return sqliteStorage.iterate(includeData, metaDataToInclude);
    }

    @Override
    public boolean isEmpty() {
        postgresStorage.isEmpty();
        return sqliteStorage.isEmpty();
    }

    @Override
    public byte[] readBytes(int id) {
        postgresStorage.readBytes(id);
        return sqliteStorage.readBytes(id);
    }

    @Override
    public void update(int id, Persistable p) {
        postgresStorage.update(id, p);
        sqliteStorage.update(id, p);
    }

    @Override
    public void remove(int id) {
        postgresStorage.remove(id);
        sqliteStorage.remove(id);
    }

    @Override
    public void remove(Persistable p) {
        postgresStorage.remove(p);
        sqliteStorage.remove(p);
    }

    @Override
    public void removeAll() {
        postgresStorage.removeAll();
        sqliteStorage.removeAll();
    }


    public Vector<Integer> removeAll(Vector<Integer> toRemove) {
        postgresStorage.removeAll(toRemove);
        return sqliteStorage.removeAll(toRemove);
    }

    // not yet implemented
    @Override
    public Vector<Integer> removeAll(EntityFilter ef) {
        postgresStorage.removeAll(ef);
        return sqliteStorage.removeAll(ef);
    }

    @Override
    public Iterator<T> iterator() {
        postgresStorage.iterator();
        return sqliteStorage.iterator();
    }

    public void getIDsForValues(String[] namesToMatch, String[] valuesToMatch, LinkedHashSet<Integer> ids) {
        postgresStorage.getIDsForValues(namesToMatch, valuesToMatch, ids);
        sqliteStorage.getIDsForValues(namesToMatch, valuesToMatch, ids);
    }

    /**
     * @param dbEntryId Set the deserialized persistable's id to the database entry id.
     *                  Doing so now is more effecient then during writes
     */
    public T newObject(InputStream serializedObjectInputStream, int dbEntryId) {
        postgresStorage.newObject(serializedObjectInputStream, dbEntryId);
        return sqliteStorage.newObject(serializedObjectInputStream, dbEntryId);
    }

    /**
     * @param dbEntryId Set the deserialized persistable's id to the database entry id.
     *                  Doing so now is more effecient then during writes
     */
    public T newObject(byte[] serializedObjectAsBytes, int dbEntryId) {
        postgresStorage.newObject(serializedObjectAsBytes, dbEntryId);
        return sqliteStorage.newObject(serializedObjectAsBytes, dbEntryId);
    }

    public void bulkRead(LinkedHashSet<Integer> cuedCases, HashMap<Integer, T> recordMap) throws RequestAbandonedException {
        postgresStorage.bulkRead(cuedCases, recordMap);
        sqliteStorage.bulkRead(cuedCases, recordMap);
    }

    @Override
    public String[] getMetaDataForRecord(int recordId, String[] metaFieldNames) {
        postgresStorage.getMetaDataForRecord(recordId, metaFieldNames);
        return sqliteStorage.getMetaDataForRecord(recordId, metaFieldNames);
    }

    @Override
    public void bulkReadMetadata(LinkedHashSet<Integer> recordIds, String[] metaFieldNames, HashMap<Integer, String[]> metadataMap) {
        postgresStorage.bulkReadMetadata(recordIds, metaFieldNames, metadataMap);
        sqliteStorage.bulkReadMetadata(recordIds, metaFieldNames, metadataMap);
    }


    /**
     * Retrieves a set of the models in storage based on a list of values matching one if the
     * indexes of this storage
     */
    public List<T> getBulkRecordsForIndex(String indexName, Collection<String> matchingValues) {
        postgresStorage.getBulkRecordsForIndex(indexName, matchingValues);
        return sqliteStorage.getBulkRecordsForIndex(indexName, matchingValues);
    }

    public String getMetaDataFieldForRecord(int recordId, String rawFieldName) {
        postgresStorage.getMetaDataFieldForRecord(recordId, rawFieldName);
        return sqliteStorage.getMetaDataFieldForRecord(recordId, rawFieldName);
    }
}
