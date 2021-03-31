package org.commcare.formplayer.sandbox;

import org.assertj.core.api.Assertions;
import org.commcare.formplayer.services.ConnectionHandler;
import org.commcare.formplayer.util.FormplayerSentry;
import org.javarosa.core.model.condition.RequestAbandonedException;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.InvalidIndexException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.sentry.SentryLevel;

/**
 * @author $|-|!˅@M
 */
public class SqlStorageWrapper<T extends Persistable>
        implements IStorageUtilityIndexed<T>, Iterable<T> {

    private SqlStorage<T> sqliteStorage;
    private SqlStorage<T> postgresStorage;
    private MeterRegistry meterRegistry;
    private boolean usePostgresResult;

    public SqlStorageWrapper(ConnectionHandler sqliteConnection, ConnectionHandler postgresConnection,
                             Class<T> prototype, String tableName, MeterRegistry meterRegistry,
                             boolean usePostgresResult) {
        sqliteStorage = new SqlStorage<T>(sqliteConnection, prototype, tableName);
        postgresStorage = new SqlStorage<T>(postgresConnection, prototype, tableName);
        this.meterRegistry = meterRegistry;
        this.usePostgresResult = usePostgresResult;
    }

    private void compareRunnable(String tag, Runnable sqliteOperation, Runnable postgresOperation) {
        long start = System.currentTimeMillis();
        sqliteOperation.run();
        long sqliteTime = System.currentTimeMillis() - start;
        meterRegistry
                .timer("storage.sqlite." + tag)
                .record(sqliteTime, TimeUnit.MILLISECONDS);

        start = System.currentTimeMillis();
        postgresOperation.run();
        long postgresTime = System.currentTimeMillis() - start;
        meterRegistry
                .timer("storage.postgres." + tag)
                .record(postgresTime, TimeUnit.MILLISECONDS);

        // Record diff
        meterRegistry
                .timer("storage.diff." + tag)
                .record(postgresTime - sqliteTime, TimeUnit.MILLISECONDS);
    }

    private <RESULT> void compareResult(RESULT first, RESULT second, String tag) {
        try {
            Assertions.assertThat(first).usingRecursiveComparison().isEqualTo(second);
            meterRegistry.counter("storage.comparison.equals").increment();
        } catch (AssertionError e) {
            meterRegistry.counter("storage.comparison.failures", tag, e.getMessage()).increment();
            FormplayerSentry.captureException(new Exception(e), SentryLevel.WARNING);
        }
    }

    private <RESULT> RESULT compareCallable(String tag, Callable<RESULT> sqliteOperation, Callable<RESULT> postgresOperation) {
        return compareCallable(tag, sqliteOperation, postgresOperation, true);
    }

    private <RESULT> RESULT compareCallable(String tag, Callable<RESULT> sqliteOperation, Callable<RESULT> postgresOperation, boolean compare) {
        RESULT result = null;
        RESULT postgresResult = null;
        long sqliteTime;
        long postgresTime;
        try {
            long start = System.currentTimeMillis();
            result = sqliteOperation.call();
            sqliteTime = System.currentTimeMillis() - start;
            meterRegistry
                    .timer("storage.sqlite." + tag)
                    .record(sqliteTime, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            long start = System.currentTimeMillis();
            postgresResult = postgresOperation.call();
            postgresTime = System.currentTimeMillis() - start;
            meterRegistry
                    .timer("storage.postgres." + tag)
                    .record(postgresTime, TimeUnit.MILLISECONDS);

            Timer diffTimer = meterRegistry.timer("storage.diff." + tag);
            diffTimer.record(postgresTime - sqliteTime, TimeUnit.MILLISECONDS);

            if (compare) {
                compareResult(result, postgresResult, tag);
            }
        } catch (Exception e) {
            FormplayerSentry.captureException(new Exception(e), SentryLevel.ERROR);
        }
        return usePostgresResult ? postgresResult : result;
    }

    private long writePostgres(Runnable operation) {
        long start = System.currentTimeMillis();
        operation.run();
        long postgresTime = System.currentTimeMillis() - start;
        meterRegistry
                .timer("storage.postgres.write")
                .record(postgresTime, TimeUnit.MILLISECONDS);
        return postgresTime;
    }

    @Override
    public void write(Persistable p) {
        long start = System.currentTimeMillis();
        sqliteStorage.write(p);
        long sqliteTime = System.currentTimeMillis() - start;

        meterRegistry
                .timer("storage.sqlite.write")
                .record(sqliteTime, TimeUnit.MILLISECONDS);

        // Retry postgres twice
        long postgresTime;
        Runnable postgresWrite = () -> postgresStorage.write(p);
        try {
            postgresTime = writePostgres(postgresWrite);
        } catch (Exception e) {
            meterRegistry
                    .counter("storage.postgres.write.failures.once")
                    .increment();
            FormplayerSentry.captureException(new Exception(e), SentryLevel.ERROR);
            try {
                postgresTime = writePostgres(postgresWrite);
            } catch (Exception ex) {
                meterRegistry
                        .counter("storage.postgres.write.failures.twice")
                        .increment();
                FormplayerSentry.captureException(new Exception(ex), SentryLevel.ERROR);
                return;
            }
        }
        meterRegistry
                .timer("storage.diff.write")
                .record(postgresTime - sqliteTime, TimeUnit.MILLISECONDS);
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
    public List<Integer> getIDsForValues(String[] metaFieldNames, Object[] values, String[] inverseFieldNames, Object[] inverseValues, LinkedHashSet<Integer> returnSet) {
        return compareCallable(
                "getIds.multiValues.fieldNames",
                () -> sqliteStorage.getIDsForValues(metaFieldNames, values, inverseFieldNames, inverseValues,returnSet),
                () -> postgresStorage.getIDsForValues(metaFieldNames, values, inverseFieldNames, inverseValues, returnSet)
        );
    }

    @Override
    public T getRecordForValue(String fieldName, Object value)
            throws NoSuchElementException, InvalidIndexException {
        String tag = "getRecord.singleValue";
        T result;
        T postgresResult = null;
        long sqliteTime;
        long postgresTime;
        try {
            long start = System.currentTimeMillis();
            result = sqliteStorage.getRecordForValue(fieldName, value);
            sqliteTime = System.currentTimeMillis() - start;
            meterRegistry
                    .timer("storage.sqlite." + tag)
                    .record(sqliteTime, TimeUnit.MILLISECONDS);
        } catch (NoSuchElementException | InvalidIndexException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            long start = System.currentTimeMillis();
            postgresResult = postgresStorage.getRecordForValue(fieldName, value);
            postgresTime = System.currentTimeMillis() - start;
            meterRegistry
                    .timer("storage.postgres." + tag)
                    .record(postgresTime, TimeUnit.MILLISECONDS);

            Timer diffTimer = meterRegistry.timer("storage.diff." + tag);
            diffTimer.record(postgresTime - sqliteTime, TimeUnit.MILLISECONDS);

            compareResult(result, postgresResult, tag);
        } catch (Exception e) {
            FormplayerSentry.captureException(new Exception(e), SentryLevel.WARNING);
        }
        return usePostgresResult ? postgresResult : result;
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
                () -> postgresStorage.iterate(),
                false // Unfortunately, there isn't an easy way to compare 2 Jdbciterators.
        );
    }

    @Override
    public JdbcSqlStorageIterator<T> iterate(boolean includeData) {
        return compareCallable(
                "iterate.includeData",
                () -> sqliteStorage.iterate(includeData),
                () -> postgresStorage.iterate(includeData),
                false
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
