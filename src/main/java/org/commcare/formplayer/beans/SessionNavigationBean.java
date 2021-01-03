package org.commcare.formplayer.beans;
import org.commcare.formplayer.objects.QueryData;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Arrays;
import java.util.Hashtable;

/**
 * Request body for navigating CommCare menus, including form preview and case selections
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionNavigationBean extends InstallRequestBean {
    private String[] selections;
    private int offset;
    private String searchText;
    private String geoLocation;
    private String menuSessionId;
    private QueryData queryData;
    private boolean isPersistent;
    private int sortIndex;

    public String[] getSelections() {
        return selections;
    }

    public void setSelections(String[] selections) {
        this.selections = selections;
    }

    @Override
    public String toString() {
        return "SessionNavigationBean [id= " + menuSessionId +
                ", selections=" + Arrays.toString(selections) +
                ", parent="  + super.toString() +
                ", queryData" + queryData + "]";
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @JsonGetter(value = "search_text")
    public String getSearchText() {
        return searchText;
    }
    @JsonSetter(value = "search_text")
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }
    @JsonGetter(value = "menu_session_id")
    public String getMenuSessionId() {
        return menuSessionId;
    }
    @JsonSetter(value = "menu_session_id")
    public void setMenuSessionId(String menuSessionId) {
        this.menuSessionId = menuSessionId;
    }
    @JsonGetter(value = "query_data")
    public QueryData getQueryData() {
        return queryData;
    }
    @JsonSetter(value = "query_data")
    public void setQueryData(QueryData queryData) {
        this.queryData = queryData;
    }

    @JsonGetter(value = "geo_location")
    public String getGeoLocation() {
        return geoLocation;
    }

    @JsonSetter(value = "geo_location")
    public void setGeoLocation(String geoLocation) {
        this.geoLocation = geoLocation;
    }

    public boolean getIsPersistent() {
        return isPersistent;
    }

    public void setIsPersistent(boolean persistent) {
        isPersistent = persistent;
    }

    public int getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(int sortIndex) {
        this.sortIndex = sortIndex;
    }
}
