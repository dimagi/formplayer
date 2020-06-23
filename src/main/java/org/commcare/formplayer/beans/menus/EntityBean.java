package org.commcare.formplayer.beans.menus;

import java.util.Arrays;

/**
 * Represents a single entity in a response list
 */
public class EntityBean {
    private String id;
    private Object[] data;

    public EntityBean(){}

    public EntityBean(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Object[] getData() {
        return data;
    }

    public void setData(Object[] data) {
        this.data = data;
    }

    @Override
    public String toString(){
        return "EntityBean with id=" + id + ", data=" + Arrays.toString(data);
    }
}
