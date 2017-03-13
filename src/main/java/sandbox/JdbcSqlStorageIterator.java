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

    public JdbcSqlStorageIterator(ArrayList<E> backingList) {
        this.backingList = backingList;
        index = 0;
    }

    @Override
    public int numRecords() {
        return backingList.size();
    }


    @Override
    public int peekID() {
        return backingList.get(index).getID();
    }

    @Override
    public int nextID() {
        int id = peekID();
        if (index + 1 < backingList.size()) {
            index++;
        }
        return id;
    }

    @Override
    public E nextRecord() {
        E e = backingList.get(index);
        index++;
        return e;
    }

    @Override
    public boolean hasMore() {
        return index < backingList.size();
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
        //unsupported
    }
}
