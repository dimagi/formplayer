package beans.menus;

import org.javarosa.core.services.locale.Localization;

import java.util.Arrays;

/**
 * Created by willpride on 4/13/16.
 */
public class MenuBean extends BaseResponseBean{

    private String[] locales;
    private String[] breadcrumbs;
    private String menuSessionId;

    @Override
    public String toString(){
        return "MenuBean [title=" + title + ", breadcrumbs: " + Arrays.toString(breadcrumbs) +
                "super=" + super.toString() + "]";
    }

    public String[] getLocales() {
        return Localization.getGlobalLocalizerAdvanced().getAvailableLocales();
    }

    public void setLocales(String[] locales) {
        this.locales = locales;
    }

    public String[] getBreadcrumbs() {
        return breadcrumbs;
    }

    public void setBreadcrumbs(String[] breadcrumbs) {
        this.breadcrumbs = breadcrumbs;
    }
    public String getMenuSessionId() {
        return menuSessionId;
    }

    public void setMenuSessionId(String menuSessionId) {
        this.menuSessionId = menuSessionId;
    }
}
