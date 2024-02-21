package org.commcare.formplayer.beans.menus;

import java.util.Arrays;

/**
 * Represents a single entity in a response list
 */
public class EntityBean {
    private String id;
    private Object[] data;

    private String groupKey;
    private String[] altText;

    public EntityBean() {
    }

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

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public String[] getAltText() {
        return altText;
    }

    public void setAltText(String[] altText) {
        this.altText = altText;
    }

    @Override
    public String toString() {
        return "EntityBean with id=" + id + ", data=" + Arrays.toString(data);
    }
}
