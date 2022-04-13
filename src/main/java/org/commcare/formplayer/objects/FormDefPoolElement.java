package org.commcare.formplayer.objects;

import org.javarosa.core.model.FormDef;

/**
 * Used in the context of the FormDefPool
 * An object to hold a FormDef as well as whether it is currently leased or not
 */
public class FormDefPoolElement {
    public FormDef formDef;
    public Boolean leased;

    public FormDefPoolElement(FormDef formDef) {
        this.formDef = formDef;
        this.leased = false;
    }
}
