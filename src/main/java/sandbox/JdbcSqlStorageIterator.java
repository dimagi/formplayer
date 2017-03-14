package sandbox;

import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.Persistable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Simple implemenation of IStorageIterator that is actually just backed by an ArrayList
 * since we don't want to deal with Connections or ResultSets lingering around.
 *
 * @author wspride
 */
public class JdbcSqlStorageIterator<E extends Persistable> implements IStorageIterator<E>, Iterator<E> {
    final private ArrayList<E> backingList;
    private int index;
    private boolean hasMore;

    public JdbcSqlStorageIterator(ArrayList<E> backingList) {
        this.backingList = backingList;
        index = 0;
        hasMore = index < this.backingList.size();
    }

    @Override
    public int numRecords() {
        return backingList.size();
    }


    @Override
    public int peekID() {
        if (index >= backingList.size()) {
            return -1;
        }
        return backingList.get(index).getID();
    }

    @Override
    public int nextID() {
        int id = peekID();
        index++;
        hasMore = index < backingList.size();
        return id;
    }

    @Override
    public E nextRecord() {
        E e = backingList.get(index);
        index++;
        hasMore = index < this.backingList.size();
        return e;
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
        throw new RuntimeException("Tried to use undefined remove() function in JdbcSqlStorageIterator");
    }

    @Override
    public String toString() {
        return "JDBCIterator with items " + backingList;
    }
}
