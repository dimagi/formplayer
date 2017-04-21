
package sandbox;

import org.javarosa.core.services.storage.Persistable;

import java.util.ArrayList;

/**
 * Iterator that only processes id indexes, and not real data.
 * 
 * @author ctsims
 * @author wspride
 */
public class JdbcSqlIndexIterator extends AbstractSqlIterator {

    public JdbcSqlIndexIterator(ArrayList<Integer> backingList) {
        super(backingList);
    }

    @Override
    Persistable getCurrentRecord() {
        throw new RuntimeException("Dynamic Fetch not yet implemented for index iterators");
    }

    @Override
    int getCurrentId() {
        return (Integer) backingList.get(index);
    }
}
