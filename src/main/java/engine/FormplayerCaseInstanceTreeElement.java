package engine;

import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.cases.model.Case;
import org.commcare.cases.util.PredicateProfile;
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
    protected Vector<Integer> getNextIndexMatch(Vector<PredicateProfile> profiles,
                                                IStorageUtilityIndexed<?> storage) throws IllegalArgumentException {
        throw new IllegalArgumentException();
    }
}
