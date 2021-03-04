package org.commcare.formplayer.sandbox;

import org.commcare.formplayer.services.ConnectionHandler;
import org.javarosa.core.model.condition.RequestAbandonedException;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.InvalidIndexException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Timer;
import io.micrometer.datadog.DatadogMeterRegistry;

/**
 * @author $|-|!˅@M
 */
public class SqlStorageWrapper<T extends Persistable>
        implements IStorageUtilityIndexed<T>, Iterable<T> {

    private SqlStorage<T> sqliteStorage;
    private SqlStorage<T> postgresStorage;
    private DatadogMeterRegistry meterRegistry;

    public SqlStorageWrapper(ConnectionHandler sqliteConnection, ConnectionHandler postgresConnection,
                             Class<T> prototype, String tableName, DatadogMeterRegistry meterRegistry) {
        sqliteStorage = new SqlStorage<T>(sqliteConnection, prototype, tableName);
        postgresStorage = new SqlStorage<T>(postgresConnection, prototype, tableName);
        this.meterRegistry = meterRegistry;
    }

    private void compareRunnable(String tag, Runnable sqliteOperation, Runnable postgresOperation) {
        Timer sqliteTimer = meterRegistry.timer("timer.sqlite." + tag);
        sqliteTimer.record(sqliteOperation);

        Timer postgresTimer = meterRegistry.timer("timer.postgres." + tag);
        postgresTimer.record(postgresOperation);
    }

    private <RESULT> RESULT compareCallable(String tag, Callable<RESULT> sqliteOperation, Callable<RESULT> postgresOperation) {
        RESULT result = null;
        try {
            long start = System.currentTimeMillis();
            result = sqliteOperation.call();
            long sqliteTime = System.currentTimeMillis() - start;

            start = System.currentTimeMillis();
            postgresOperation.call();
            long postgresTime = System.currentTimeMillis() - start;

            Timer diffTimer = meterRegistry.timer("storage.timing." + tag);
            diffTimer.record(postgresTime - sqliteTime, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public void write(Persistable p) {
        compareRunnable(
                "write",
                () -> sqliteStorage.write(p),
                () -> postgresStorage.write(p)
        );
    }

    @Override
    public T read(int id) {
        return compareCallable(
              "read",
                () -> sqliteStorage.read(id),
                () -> postgresStorage.read(id)
        );
    }

    @Override
    public Vector<Integer> getIDsForValue(String fieldName, Object value) {
        return compareCallable(
                "getIds.singleValue",
                () -> sqliteStorage.getIDsForValue(fieldName, value),
                () -> postgresStorage.getIDsForValue(fieldName, value)
        );
    }

    @Override
    public List<Integer> getIDsForValues(String[] fieldNames, Object[] values) {
        return compareCallable(
                "getIds.multiValues",
                () -> sqliteStorage.getIDsForValues(fieldNames, values),
                () -> postgresStorage.getIDsForValues(fieldNames, values)
        );
    }

    @Override
    public List<Integer> getIDsForValues(String[] fieldNames, Object[] values, LinkedHashSet<Integer> returnSet) {
        return compareCallable(
                "getIds.multiValues.returnSet",
                () -> sqliteStorage.getIDsForValues(fieldNames, values, returnSet),
                () -> postgresStorage.getIDsForValues(fieldNames, values, returnSet)
        );
    }

    @Override
    public T getRecordForValue(String fieldName, Object value)
            throws NoSuchElementException, InvalidIndexException {
        T result;
        try {
            Timer sqliteTimer = meterRegistry.timer("timer.sqlite.getRecord.singleValue");
            result = sqliteTimer.recordCallable(
                    () -> sqliteStorage.getRecordForValue(fieldName, value)
            );

            Timer postgresTimer = meterRegistry.timer("timer.postgres.getRecord.singleValue");
            postgresTimer.recordCallable(
                    () -> postgresStorage.getRecordForValue(fieldName, value)
            );
        } catch (NoSuchElementException | InvalidIndexException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public Vector<T> getRecordsForValues(String[] metaFieldNames, Object[] values) {
        return compareCallable(
                "getRecord.multiValue",
                () -> sqliteStorage.getRecordsForValues(metaFieldNames, values),
                () -> postgresStorage.getRecordsForValues(metaFieldNames, values)
        );
    }

    @Override
    public int add(T e) {
        return compareCallable(
                "add",
                () -> sqliteStorage.add(e),
                () -> postgresStorage.add(e)
        );
    }

    @Override
    public void close() {
        // Don't need this because we close all resources after using them
    }

    @Override
    public boolean exists(int id) {
        return compareCallable(
                "exists",
                () -> sqliteStorage.exists(id),
                () -> postgresStorage.exists(id)
        );
    }

    @Override
    public Object getAccessLock() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getNumRecords() {
        return compareCallable(
                "numRecords",
                () -> sqliteStorage.getNumRecords(),
                () -> postgresStorage.getNumRecords()
        );
    }

    @Override
    public JdbcSqlStorageIterator<T> iterate() {
        return compareCallable(
                "iterate",
                () -> sqliteStorage.iterate(),
                () -> postgresStorage.iterate()
        );
    }

    @Override
    public JdbcSqlStorageIterator<T> iterate(boolean includeData) {
        return compareCallable(
                "iterate.includeData",
                () -> sqliteStorage.iterate(includeData),
                () -> postgresStorage.iterate(includeData)
        );
    }

    @Override
    public boolean isEmpty() {
        return compareCallable(
                "isEmpty",
                () -> sqliteStorage.isEmpty(),
                () -> postgresStorage.isEmpty()
        );
    }

    @Override
    public byte[] readBytes(int id) {
        return compareCallable(
                "readBytes",
                () -> sqliteStorage.readBytes(id),
                () -> postgresStorage.readBytes(id)
        );
    }

    @Override
    public void update(int id, Persistable p) {
        compareRunnable(
                "update",
                () -> sqliteStorage.update(id, p),
                () -> postgresStorage.update(id, p)
        );
    }

    @Override
    public void remove(int id) {
        compareRunnable(
                "removeId",
                () -> sqliteStorage.remove(id),
                () -> postgresStorage.remove(id)
        );
    }

    @Override
    public void remove(Persistable p) {
        compareRunnable(
                "removeResource",
                () -> sqliteStorage.remove(p),
                () -> postgresStorage.remove(p)
        );
    }

    @Override
    public void removeAll() {
        compareRunnable(
                "removeAll",
                () -> sqliteStorage.removeAll(),
                () -> postgresStorage.removeAll()
        );
    }


    public Vector<Integer> removeAll(Vector<Integer> toRemove) {
        return compareCallable(
                "removeAll.ids",
                () -> sqliteStorage.removeAll(toRemove),
                () -> postgresStorage.removeAll(toRemove)
        );
    }

    // not yet implemented
    @Override
    public Vector<Integer> removeAll(EntityFilter ef) {
        return compareCallable(
                "removeAll.entityFilter",
                () -> sqliteStorage.removeAll(ef),
                () -> postgresStorage.removeAll(ef)
        );
    }

    @Override
    public Iterator<T> iterator() {
        return compareCallable(
                "iterator",
                () -> sqliteStorage.iterator(),
                () -> postgresStorage.iterator()
        );
    }

    public void bulkRead(LinkedHashSet<Integer> cuedCases, HashMap<Integer, T> recordMap) throws RequestAbandonedException {
        compareRunnable(
                "bulkRead",
                () -> sqliteStorage.bulkRead(cuedCases, recordMap),
                () -> postgresStorage.bulkRead(cuedCases, recordMap)
        );
    }

    @Override
    public String[] getMetaDataForRecord(int recordId, String[] metaFieldNames) {
        return compareCallable(
                "getMetaData",
                () -> sqliteStorage.getMetaDataForRecord(recordId, metaFieldNames),
                () -> postgresStorage.getMetaDataForRecord(recordId, metaFieldNames)
        );
    }

    @Override
    public void bulkReadMetadata(LinkedHashSet<Integer> recordIds, String[] metaFieldNames, HashMap<Integer, String[]> metadataMap) {
        compareRunnable(
                "bulkReadMetadata",
                () -> sqliteStorage.bulkReadMetadata(recordIds, metaFieldNames, metadataMap),
                () -> postgresStorage.bulkReadMetadata(recordIds, metaFieldNames, metadataMap)
        );
    }
}
