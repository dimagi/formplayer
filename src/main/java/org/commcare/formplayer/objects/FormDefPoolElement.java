package org.commcare.formplayer.objects;

import org.javarosa.core.model.FormDef;

public class FormDefPoolElement {
    public FormDef formDef;
    public Boolean leased;

    public FormDefPoolElement(FormDef formDef) {
        this.formDef = formDef;
        this.leased = false;
    }
}
