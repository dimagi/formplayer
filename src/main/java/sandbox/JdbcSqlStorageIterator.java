package sandbox;

import org.javarosa.core.services.storage.Persistable;

import java.util.ArrayList;

/**
 * Simple implemenation of IStorageIterator that is actually just backed by an ArrayList
 * since we don't want to deal with Connections or ResultSets lingering around.
 *
 * @author wspride
 */
public class JdbcSqlStorageIterator <E extends Persistable> extends AbstractSqlIterator {

    public JdbcSqlStorageIterator(ArrayList<E> backingList) {
        super(backingList);
    }

    E getCurrentRecord() {
        return ((E)backingList.get(index));
    }

    @Override
    int getCurrentId() {
        return getCurrentRecord().getID();
    }

}
