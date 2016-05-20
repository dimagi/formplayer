package beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Arrays;

/**
 * Created by willpride on 4/28/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionNavigationBean extends InstallRequestBean {
    private String[] selections;
    private int offset;

    public String[] getSelections() {
        return selections;
    }

    public void setSelections(String[] selections) {
        this.selections = selections;
    }

    @Override
    public String toString() {
        return "SessionNavigationBean [selections="
                + Arrays.toString(selections) +  " parent="  + super.toString() + "]";
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
