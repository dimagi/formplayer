package org.commcare.formplayer.beans.menus;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.suite.model.DisplayUnit;
import org.commcare.util.screen.QueryScreen;

import org.javarosa.core.util.OrderedHashtable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by willpride on 4/13/16.
 */
public class QueryResponseBean extends MenuBean {

    private DisplayElement[] displays;
    private final String type = "query";

    QueryResponseBean(){}

    public DisplayElement[] getDisplays() {
        return displays;
    }

    private void setDisplays(DisplayElement[] displays) {
        this.displays = displays;
    }

    public QueryResponseBean(QueryScreen queryScreen, SessionWrapper session){
        OrderedHashtable<String, DisplayUnit> displayMap = queryScreen.getUserInputDisplays();
        displays = new DisplayElement[displayMap.size()];
        int count = 0 ;
        for (String key : Collections.list(displayMap.keys())) {
            displays[count] = new DisplayElement(displayMap.get(key),
                    session.getEvaluationContext(),
                    key);
            count++;
        }
        setTitle(queryScreen.getScreenTitle());
    }

    @Override
    public String toString(){
        return "QueryResponseBean [displays=" + Arrays.toString(displays)
                + "MenuBean= " + super.toString() + "]";
    }

    public String getType() {
        return type;
    }
}
