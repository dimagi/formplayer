package beans.menus;

import org.javarosa.core.services.locale.Localization;

/**
 * Created by willpride on 4/13/16.
 */
public class MenuBean {

    private String title;
    private String[] locales;
    private String menuSessionId;

    public String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString(){
        return "MenuBean [title=" + title + "]";
    }

    public String[] getLocales() {
        return Localization.getGlobalLocalizerAdvanced().getAvailableLocales();
    }

    public void setLocales(String[] locales) {
        this.locales = locales;
    }

    public String getMenuSessionId() {
        return menuSessionId;
    }

    public void setMenuSessionId(String menuSessionId) {
        this.menuSessionId = menuSessionId;
    }
}
