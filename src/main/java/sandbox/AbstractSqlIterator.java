package sandbox;

import org.commcare.modern.database.TableBuilder;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.Persistable;

import java.util.*;

/**
 * Abstract implementation for IStorageIterator that uses a backing ArrayList of a generic type
 * To implement just need to specify how this generic type maps to a record E and an index int
 */
public abstract class AbstractSqlIterator <E extends Persistable> implements IStorageIterator<E>, Iterator<E> {

    final protected ArrayList<?> backingList;
    protected int index;
    protected boolean hasMore;

    private final Set<String> metaDataIndexSet;
    private HashMap<String, Integer> metaDataColumnMap = new HashMap<>();

    public AbstractSqlIterator(ArrayList<?> backingList) {
        this.backingList = backingList;
        index = 0;
        hasMore = index < this.backingList.size();
        metaDataIndexSet = new HashSet<>();
    }

    abstract E getCurrentRecord();
    abstract int getCurrentId();

    @Override
    public int peekID() {
        if (index >= backingList.size()) {
            return -1;
        }
        return getCurrentId();
    }

    @Override
    public E nextRecord() {
        E e = getCurrentRecord();
        index++;
        hasMore = index < this.backingList.size();
        return e;
    }

    @Override
    public int numRecords() {
        return backingList.size();
    }

    @Override
    public int nextID() {
        int id = peekID();
        index++;
        hasMore = index < backingList.size();
        return id;
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
        if (!metaDataIndexSet.contains(metadataKey)) {
            throw new RuntimeException("Invalid iterator metadata request for key: " + metadataKey);
        }
        int columnIndex;
        if (metaDataColumnMap.containsKey(metadataKey)) {
            columnIndex = metaDataColumnMap.get(metadataKey);
        } else {
            String columnName = TableBuilder.scrubName(metadataKey);
            columnIndex = c.getColumnIndexOrThrow(columnName);
            metaDataColumnMap.put(metadataKey, columnIndex);
        }
        return c.getString(columnIndex);
    }

    @Override
    public boolean hasMore() {
        return hasMore;
    }

    @Override
    public boolean hasNext() {
        return hasMore();
    }

    @Override
    public E next() {
        return nextRecord();
    }

    @Override
    public void remove() {
        throw new RuntimeException("Tried to use undefined remove() function in AbstractSqlIterator");
    }

    @Override
    public String toString() {
        return "AbstractSqlIterator with items " + backingList;
    }
}
