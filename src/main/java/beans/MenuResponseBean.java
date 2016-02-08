package beans;

import java.util.Arrays;
import java.util.Map;

/**
 * Created by willpride on 2/5/16.
 */
public class MenuResponseBean extends SessionBean{
    private String menuType;
    private Map<Integer, String> options;

    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }

    public Map<Integer, String> getOptions() {
        return options;
    }

    public void setOptions(Map<Integer, String> options) {
        this.options = options;
    }

    @Override
    public String toString(){
        return "MenuResponseBean [menuType=" + menuType + ", options=" + options;
    }
}
