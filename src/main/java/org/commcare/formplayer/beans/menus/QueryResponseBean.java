package org.commcare.formplayer.beans.menus;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.suite.model.QueryPrompt;
import org.commcare.util.screen.QueryScreen;
import org.javarosa.core.util.OrderedHashtable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;

/**
 * Created by willpride on 4/13/16.
 */
public class QueryResponseBean extends MenuBean {

    private DisplayElement[] displays;
    private String queryKey;
    private final String type = "query";

    QueryResponseBean() {
    }

    public DisplayElement[] getDisplays() {
        return displays;
    }

    private void setDisplays(DisplayElement[] displays) {
        this.displays = displays;
    }

    public QueryResponseBean(QueryScreen queryScreen, SessionWrapper session) {
        OrderedHashtable<String, QueryPrompt> queryPromptMap = queryScreen.getUserInputDisplays();
        Hashtable<String, String> currentAnswers = queryScreen.getCurrentAnswers();
        displays = new DisplayElement[queryPromptMap.size()];
        int count = 0;
        for (String key : Collections.list(queryPromptMap.keys())) {
            QueryPrompt queryPromptItem = queryPromptMap.get(key);
            String currentAnswer = currentAnswers.get(key);

            // Map the current Answer to the itemset index of the answer
            Pair<String[], Integer> choicesAndAnswerIndex = queryPromptItem.getItemsetChoicesWithAnswerIndex(currentAnswer);
            if (queryPromptItem.isSelectOne()) {
                currentAnswer = choicesAndAnswerIndex.second == -1 ? null : String.valueOf(choicesAndAnswerIndex.second);
            }

            displays[count] = new DisplayElement(queryPromptItem.getDisplay(),
                    session.getEvaluationContext(),
                    key,
                    queryPromptItem.getInput(),
                    queryPromptItem.getReceive(),
                    currentAnswer,
                    choicesAndAnswerIndex.first);
            count++;
        }
        setTitle(queryScreen.getScreenTitle());
        this.queryKey = session.getCommand();
    }

    @Override
    public String toString() {
        return "QueryResponseBean [displays=" + Arrays.toString(displays)
                + "MenuBean= " + super.toString() + "]";
    }

    public String getType() {
        return type;
    }

    @JsonGetter(value="queryKey")
    public String getQueryKey() {
        return queryKey;
    }
    @JsonSetter(value="queryKey")
    public void setQueryKey(String queryKey) {
        this.queryKey = queryKey;
    }
}
