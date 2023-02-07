package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import org.commcare.formplayer.objects.QueryData;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Request body for navigating CommCare menus, including form preview and case selections
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionNavigationBean extends InstallRequestBean {
    private String[] selections;
    private int offset;
    private String endpointId;
    private HashMap<String, String> endpointArgs;
    private String searchText;
    private String geoLocation;
    private QueryData queryData;
    private boolean isPersistent;
    private int sortIndex;
    private int casesPerPage;
    private String[] selectedValues;

    public String[] getSelections() {
        return selections;
    }

    public void setSelections(String[] selections) {
        this.selections = selections;
    }

    @Override
    public String toString() {
        return "SessionNavigationBean [" +
                ", selections=" + Arrays.toString(selections) +
                ", parent=" + super.toString() +
                ", queryData" + queryData + "]";
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @JsonGetter(value = "endpoint_id")
    public String getEndpointId() {
        return endpointId;
    }

    @JsonSetter(value = "endpoint_id")
    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    @JsonGetter(value = "endpoint_args")
    public HashMap<String, String> getEndpointArgs() {
        return endpointArgs;
    }

    @JsonSetter(value = "endpoint_args")
    public void setEndpointArgs(HashMap<String, String> endpointArgs) {
        this.endpointArgs = endpointArgs;
    }

    @JsonGetter(value = "search_text")
    public String getSearchText() {
        return searchText;
    }

    @JsonSetter(value = "search_text")
    public void setSearchText(String searchText) {
        this.searchText = searchText;
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

    @JsonGetter(value = "cases_per_page")
    public int getCasesPerPage() {
        return casesPerPage;
    }

    @JsonSetter(value = "cases_per_page")
    public void setCasesPerPage(int casesPerPage) {
        this.casesPerPage = casesPerPage;
    }

    @JsonGetter(value = "selected_values")
    public String[] getSelectedValues() {
        return selectedValues;
    }

    @JsonSetter(value = "selected_values")
    public void setSelectedValues(String[] selectedValues) {
        this.selectedValues = selectedValues;
    }
}
