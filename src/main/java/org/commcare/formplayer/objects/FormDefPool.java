package org.commcare.formplayer.objects;

import org.commcare.formplayer.exceptions.AlreadyExistsInPoolException;
import org.commcare.formplayer.exceptions.FormDefEntryNotFoundException;
import org.commcare.modern.util.Pair;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;

import java.util.*;

public class FormDefPool {

    private final Hashtable<String, Pair<TreeElement, List<FormDefPoolElement>>> pool;

    public FormDefPool() {
        this.pool = new Hashtable<>();
    }

    public void create(String id, FormDef formDef) throws AlreadyExistsInPoolException {
        Pair<TreeElement, List<FormDefPoolElement>> pair = this.pool.get(id);
        if (pair != null) {
            addFormDefToExistingEntry(pair.second, formDef);
        } else {
            createNewFormDefPoolEntry(id, formDef);
        }
    }
    public FormDef getFormDef(String id) {
        Pair<TreeElement, List<FormDefPoolElement>> pair = this.pool.get(id);
        // if an entry exists and there is an available form def to lease
        if (pair != null) {
            FormDefPoolElement availableElement = pair.second.stream()
                    .filter(e -> !e.leased)
                    .findFirst()
                    .orElse(null);
            if (availableElement != null) {
                availableElement.leased = true;
                return availableElement.formDef;
            }
        }
        return null;
    }

    public void returnFormDef(String id, FormDef formDef) throws FormDefEntryNotFoundException {
        Pair<TreeElement, List<FormDefPoolElement>> pair = this.pool.get(id);
        if (pair == null) {
            throw new FormDefEntryNotFoundException(id);
        }

        FormDef cleanedFormDef = resetFormDef(pair.first, formDef);
        pair.second.stream().filter(e -> e.formDef == cleanedFormDef).findFirst().ifPresent(element -> element.leased = false);
    }

    private FormDef resetFormDef(TreeElement templateRoot, FormDef formDef) {
        TreeElement copiedRoot = templateRoot.deepCopy(true);
        TreeReference tr = TreeReference.rootRef();
        tr.add(copiedRoot.getName(), TreeReference.INDEX_UNBOUND);
        formDef.getMainInstance().setRoot(copiedRoot);
        return formDef;
    }

    private void addFormDefToExistingEntry(List<FormDefPoolElement> elements, FormDef formDef) throws AlreadyExistsInPoolException {
        FormDefPoolElement element = elements.stream().filter(e -> e.formDef == formDef).findFirst().orElse(null);
        if (element != null) {
            throw new AlreadyExistsInPoolException();
        }
        FormDefPoolElement elementToAdd = new FormDefPoolElement(formDef);
        elements.add(elementToAdd);
    }

    private void createNewFormDefPoolEntry(String id, FormDef formDef) {
        TreeElement templateRoot = formDef.getMainInstance().getRoot().deepCopy(true);
        List<FormDefPoolElement> elements = new ArrayList<>();
        FormDefPoolElement elementToAdd = new FormDefPoolElement(formDef);
        elements.add(elementToAdd);
        this.pool.put(id, new Pair(templateRoot, elements));
    }
}
