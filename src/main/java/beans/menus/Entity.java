package beans.menus;

import java.util.Arrays;

/**
 * Created by willpride on 4/13/16.
 */
public class Entity {
    private int index;
    private Object[] data;
    private EntityDetailResponse detail;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Object[] getData() {
        return data;
    }

    public void setData(Object[] data) {
        this.data = data;
    }

    public EntityDetailResponse getDetail() {
        return detail;
    }

    public void setDetail(EntityDetailResponse detail) {
        this.detail = detail;
    }

    @Override
    public String toString(){
        return "Entity at index=" + index + ", data=" + Arrays.toString(data);
    }
}
