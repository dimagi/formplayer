package sandbox;

import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.Persistable;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Abstract implementation for IStorageIterator that uses a backing ArrayList of a generic type
 * To implement just need to specify how this generic type maps to a record E and an index int
 */
public abstract class AbstractSqlIterator <E extends Persistable> implements IStorageIterator<E>, Iterator<E> {

    final protected ArrayList<?> backingList;
    protected int index;
    protected boolean hasMore;

    public AbstractSqlIterator(ArrayList<?> backingList) {
        this.backingList = backingList;
        index = 0;
        hasMore = index < this.backingList.size();
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
