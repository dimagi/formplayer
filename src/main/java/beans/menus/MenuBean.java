package beans.menus;

import org.javarosa.core.services.locale.Localization;

import java.util.Arrays;

/**
 * Created by willpride on 4/13/16.
 */
public class MenuBean {

    private String title;
    private String[] locales;
    private String[] breadcrumbs;
    private String menuSessionId;

    private String message;
    private boolean isError;

    public String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString(){
        return "MenuBean [title=" + title + ", breadcrumbs: " + Arrays.toString(breadcrumbs) + "]";
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

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
