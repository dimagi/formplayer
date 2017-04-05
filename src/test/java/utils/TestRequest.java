package utils;

import beans.NewFormResponse;
import beans.QuestionBean;
import beans.menus.Command;
import beans.menus.CommandListResponseBean;
import beans.menus.EntityBean;
import beans.menus.EntityListResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;

/**
 * Created by willpride on 4/5/17.
 */
public class TestRequest {
    private String username;
    private String domain;
    private String appId;
    private String[] selections;
    private String oldResult;

    private static ObjectMapper objectMapper = new ObjectMapper();

    public TestRequest(String appId, String username, String domain, String selections) {
        this.appId = appId;
        this.username = username;
        this.domain = domain;
        this.selections = selections.split(" ");
    }

    public void loadNewResult(String newResult) {
        if (oldResult != null) {
            if (!testEquality(newResult, oldResult)) {
                throw new RuntimeException(String.format("New result \n%s differs from old result \n%s",
                        newResult, oldResult));
            }
        }
        setOldResult(newResult);
    }

    private static boolean testEquality(String resultOne, String resultTwo) {
        try {
            CommandListResponseBean responseOne = objectMapper.readValue(resultOne, CommandListResponseBean.class);
            CommandListResponseBean responseTwo = objectMapper.readValue(resultTwo, CommandListResponseBean.class);
            Command[] commandsOne = responseOne.getCommands();
            Command[] commandsTwo = responseTwo.getCommands();
            for (int i=0; i < commandsOne.length; i++) {
                if (!commandsOne[i].getDisplayText().equals(commandsTwo[i].getDisplayText())) {
                    System.out.println(String.format("Command %s differs from commands %s", commandsOne[i], commandsTwo[i]));
                    return false;
                }
            }
            return true;
        } catch (Exception ignored) {

        }
        try {
            EntityListResponse responseOne = objectMapper.readValue(resultOne, EntityListResponse.class);
            EntityListResponse responseTwo = objectMapper.readValue(resultTwo, EntityListResponse.class);
            EntityBean[] entitiesOne = responseOne.getEntities();
            EntityBean[] entitiesTwo = responseTwo.getEntities();
            for (int i=0; i < entitiesOne.length; i++) {
                if (!entitiesOne[i].getId().equals(entitiesTwo[i].getId())) {
                    System.out.println(String.format("Command %s differs from commands %s", entitiesOne[i], entitiesTwo[i]));
                    return false;
                }
            }
            return true;
        } catch (Exception ignored) {

        }
        try {
            NewFormResponse responseOne = objectMapper.readValue(resultOne, NewFormResponse.class);
            NewFormResponse responseTwo = objectMapper.readValue(resultTwo, NewFormResponse.class);
            QuestionBean[] treeOne= responseOne.getTree();
            QuestionBean[] treeTwo = responseTwo.getTree();
            for (int i=0; i < treeOne.length; i++) {
                if (!treeOne[i].getCaption().equals(treeTwo[i].getCaption())) {
                    System.out.println(String.format("Command %s differs from commands %s", treeOne[i], treeTwo[i]));
                    return false;
                }
            }
            return true;
        } catch (Exception ignored) {

        }
        return false;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String[] getSelections() {
        return selections;
    }

    public void setSelections(String[] selections) {
        this.selections = selections;
    }

    public String getOldResult() {
        return oldResult;
    }

    public void setOldResult(String oldResult) {
        this.oldResult = oldResult;
    }

    @Override
    public String toString() {
        return String.format("TestRequest appId=%s, username=%s, domain=%s, selections=%s",
                appId,
                username,
                domain,
                Arrays.toString(selections));
    }
}
