
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
 * Iterator that only processes id indexes, and not real data.
 * 
 * @author ctsims
 */
public class JdbcSqlIndexIterator<E extends Persistable> extends JdbcSqlStorageIterator<E> {
    final private ArrayList<Integer> idList;
    private int index;
    private boolean hasMore;

    public JdbcSqlIndexIterator(ArrayList<Integer> idList) {
        super(new ArrayList<E>());
        this.idList = idList;
        index = 0;
        hasMore = index < this.idList.size();
    }

    @Override
    public int numRecords() {
        return idList.size();
    }


    @Override
    public int peekID() {
        if (index >= idList.size()) {
            return -1;
        }
        return idList.get(index);
    }

    @Override
    public int nextID() {
        int id = peekID();
        index++;
        hasMore = index < idList.size();
        return id;
    }

    @Override
    public E nextRecord() {
        throw new RuntimeException("Dyamic Fetch not net implemented for index iterators");
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
        return "JDBCIterator with size " + numRecords();
    }
}
