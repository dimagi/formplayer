package org.commcare.formplayer.objects;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="entities_selection")
public class EntitiesSelection {

    @Id
    @GeneratedValue(generator="uuid")
    @GenericGenerator(name="uuid", strategy="org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(updatable=false)
    @Convert(converter=ByteArrayConverter.class)
    private String[] entities;

    @SuppressWarnings("unused")
    public EntitiesSelection() {
    }

    public EntitiesSelection(String[] entities) {
        this.entities = entities;
    }

    public String[] getEntities() {
        return entities;
    }

    public String getId() {
        return id;
    }
}
