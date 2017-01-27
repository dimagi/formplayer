package engine;

import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.cases.model.Case;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;

import java.util.Vector;

/**
 * Created by willpride on 1/26/17.
 */
public class FormplayerCaseInstanceTreeElement extends CaseInstanceTreeElement {

    public FormplayerCaseInstanceTreeElement(AbstractTreeElement instanceRoot, IStorageUtilityIndexed<Case> storage) {
        super(instanceRoot, storage);
    }

    @Override
    protected Vector<Integer> getNextIndexMatch(Vector<String> keys, Vector<Object> values,
                                                IStorageUtilityIndexed<?> storage) throws IllegalArgumentException {
        if (keys.isEmpty()) {
            throw new IllegalArgumentException();
        }

        String firstKey = keys.elementAt(0);
        if (firstKey.startsWith(Case.INDEX_CASE_INDEX_PRE)) {
            keys.remove(0);
            values.remove(0);
            return getNextIndexMatch(keys, values, storage);
        }

        String key = keys.elementAt(0);
        Object o = values.elementAt(0);

        //Get matches if it works
        Vector<Integer> returnValue = storage.getIDsForValue(key, o);

        //If we processed this, pop it off the queue
        keys.removeElementAt(0);
        values.removeElementAt(0);

        return returnValue;
    }
}
