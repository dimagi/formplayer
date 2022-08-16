package org.commcare.formplayer.beans.menus;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.RemoteQuerySessionManager;
import org.commcare.suite.model.QueryPrompt;
import org.commcare.suite.model.QueryPromptCondition;
import org.commcare.util.screen.QueryScreen;
import org.javarosa.core.model.utils.ItemSetUtils;
import org.javarosa.core.util.OrderedHashtable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;

/**
 * Created by willpride on 4/13/16.
 */
public class QueryResponseBean extends MenuBean {

    private DisplayElement[] displays;
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
        Hashtable<String, String> errors = queryScreen.getErrors();
        Hashtable<String, Boolean> requiredPrompts = queryScreen.getRequiredPrompts();
        displays = new DisplayElement[queryPromptMap.size()];
        int count = 0;
        for (String key : Collections.list(queryPromptMap.keys())) {
            QueryPrompt queryPromptItem = queryPromptMap.get(key);
            String currentAnswer = currentAnswers.get(key);

            // Map the current Answer to the itemset index of the answer
            String[] choiceLabels = null;
            if (queryPromptItem.isSelect()) {
                String[] selectedChoices = RemoteQuerySessionManager.extractMultipleChoices(
                        currentAnswer);
                ArrayList<String> indicesForSelectedChoices = new ArrayList<>(
                        selectedChoices.length);
                for (int i = 0; i < selectedChoices.length; i++) {
                    if (selectedChoices[i].isEmpty()) {
                        indicesForSelectedChoices.add("");
                    } else {
                        int choiceIndex = ItemSetUtils.getIndexOf(
                                queryPromptItem.getItemsetBinding(), selectedChoices[i]);
                        if (choiceIndex != -1) {
                            indicesForSelectedChoices.add(String.valueOf(choiceIndex));
                        }
                    }
                }
                if (indicesForSelectedChoices.size() > 0) {
                    currentAnswer = String.join(RemoteQuerySessionManager.ANSWER_DELIMITER,
                            indicesForSelectedChoices);
                }
                choiceLabels = ItemSetUtils.getChoiceLabels(queryPromptItem.getItemsetBinding());
            }

            String requiredMessage = queryPromptItem.getRequiredMessage(session.getEvaluationContext());
            boolean isRequired = requiredPrompts.containsKey(key) && requiredPrompts.get(key);
            displays[count] = new DisplayElement(queryPromptItem.getDisplay(),
                    session.getEvaluationContext(),
                    key,
                    queryPromptItem.getInput(),
                    queryPromptItem.getReceive(),
                    queryPromptItem.getHidden(),
                    currentAnswer,
                    choiceLabels,
                    queryPromptItem.isAllowBlankValue(),
                    isRequired,
                    requiredMessage,
                    errors.get(key)
                    );
            count++;
        }
        setTitle(queryScreen.getScreenTitle());
        setQueryKey(session.getCommand());
    }

    @Override
    public String toString() {
        return "QueryResponseBean [displays=" + Arrays.toString(displays)
                + "MenuBean= " + super.toString() + "]";
    }

    public String getType() {
        return type;
    }
}
