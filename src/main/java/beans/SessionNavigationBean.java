package beans;

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
    private String menuSessionId;
    private Hashtable<String, String> queryDictionary;
    private String previewCommand;

    public String[] getSelections() {
        return selections;
    }

    public void setSelections(String[] selections) {
        this.selections = selections;
    }

    @Override
    public String toString() {
        return "SessionNavigationBean [id= " + menuSessionId +
                ", preview=" + previewCommand +
                ", selections=" + Arrays.toString(selections) +
                ", parent="  + super.toString() + "]";
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
    @JsonGetter(value = "query_dictionary")
    public Hashtable<String, String> getQueryDictionary() {
        return queryDictionary;
    }
    @JsonSetter(value = "query_dictionary")
    public void setQueryDictionary(Hashtable<String, String> queryDictionary) {
        this.queryDictionary = queryDictionary;
    }

    public String getPreviewCommand() {
        return previewCommand;
    }

    public void setPreviewCommand(String previewCommand) {
        this.previewCommand = previewCommand;
    }
}
