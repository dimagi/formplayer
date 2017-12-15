package concurrency_tests;

import beans.NewFormResponse;
import beans.QuestionBean;
import beans.menus.Command;
import beans.menus.CommandListResponseBean;
import beans.menus.EntityBean;
import beans.menus.EntityListResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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
            try {
                if (!testEquality(newResult, oldResult)) {
                    throw new RuntimeException(String.format("New result \n%s differs from old result \n%s",
                            newResult, oldResult));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        setOldResult(newResult);
    }

    private static boolean testEquality(String resultOne, String resultTwo) throws IOException, JSONException {
        JSONObject jsonResultOne = new JSONObject(resultOne);
        JSONObject jsonResultTwo = new JSONObject(resultTwo);
        if (jsonResultOne.has("nextScreen")) {
            jsonResultOne = (JSONObject) jsonResultOne.get("nextScreen");
            jsonResultTwo = (JSONObject) jsonResultTwo.get("nextScreen");
        }
        if (!jsonResultOne.has("type")) {
            NewFormResponse responseOne = objectMapper.readValue(jsonResultOne.toString(), NewFormResponse.class);
            NewFormResponse responseTwo = objectMapper.readValue(jsonResultTwo.toString(), NewFormResponse.class);
            QuestionBean[] treeOne= responseOne.getTree();
            QuestionBean[] treeTwo = responseTwo.getTree();
            for (int i=0; i < treeOne.length; i++) {
                if (!treeOne[i].getCaption().equals(treeTwo[i].getCaption())) {
                    System.out.println(String.format("Command %s differs from commands %s", treeOne[i], treeTwo[i]));
                    return false;
                }
            }
            return true;
        }
        String type = jsonResultOne.getString("type");
        if (type.equals("commands")) {
            CommandListResponseBean responseOne = objectMapper.readValue(jsonResultOne.toString(), CommandListResponseBean.class);
            CommandListResponseBean responseTwo = objectMapper.readValue(jsonResultTwo.toString(), CommandListResponseBean.class);
            Command[] commandsOne = responseOne.getCommands();
            Command[] commandsTwo = responseTwo.getCommands();
            for (int i=0; i < commandsOne.length; i++) {
                if (!commandsOne[i].getDisplayText().equals(commandsTwo[i].getDisplayText())) {
                    System.out.println(String.format("Command %s differs from commands %s", commandsOne[i], commandsTwo[i]));
                    return false;
                }
            }
            return true;
        } else if (type.equals("entities")) {
            EntityListResponse responseOne = objectMapper.readValue(jsonResultOne.toString(), EntityListResponse.class);
            EntityListResponse responseTwo = objectMapper.readValue(jsonResultTwo.toString(), EntityListResponse.class);
            EntityBean[] entitiesOne = responseOne.getEntities();
            EntityBean[] entitiesTwo = responseTwo.getEntities();
            for (int i=0; i < entitiesOne.length; i++) {
                if (!entitiesOne[i].getId().equals(entitiesTwo[i].getId())) {
                    System.out.println(String.format("Command %s differs from commands %s", entitiesOne[i], entitiesTwo[i]));
                    return false;
                }
            }
            return true;
        } else {
            throw new RuntimeException("Couldn't handle response " + jsonResultOne);
        }
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
