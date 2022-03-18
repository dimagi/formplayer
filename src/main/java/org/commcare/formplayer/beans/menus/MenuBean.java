package org.commcare.formplayer.beans.menus;

import org.javarosa.core.services.locale.Localization;

import java.util.Arrays;

/**
 * Created by willpride on 4/13/16.
 */
public class MenuBean extends BaseResponseBean {

    private String[] locales;
    private String[] breadcrumbs;
    private EntityDetailResponse persistentCaseTile;
    private String queryKey;

    @Override
    public String toString() {
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

    public EntityDetailResponse getPersistentCaseTile() {
        return persistentCaseTile;
    }

    public void setPersistentCaseTile(EntityDetailResponse persistentCaseTile) {
        this.persistentCaseTile = persistentCaseTile;
    }

    public String getQueryKey() {
        return queryKey;
    }

    public void setQueryKey(String queryKey) {
        this.queryKey = queryKey;
    }
}
