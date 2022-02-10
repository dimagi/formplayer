package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Created by willpride on 1/20/16.
 */
public class AnswerQuestionRequestBean extends SessionRequestBean {
    private String formIndex;
    // This can be an array (multi select, geo point), integer, date, or String.
    // Even though they always come in as Strings, Jackson will try to parse the String into the
    // above classes
    // and so needs this to be an Object to store them in
    private Object answer;

    private String navMode;

    @Nullable
    private Map<String, Object> answersToValidate;

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public AnswerQuestionRequestBean() {
    }

    public AnswerQuestionRequestBean(String formIndex, String answer, String sessionId) {
        this.formIndex = formIndex;
        this.answer = answer;
        this.sessionId = sessionId;
    }

    @JsonGetter(value = "ix")
    public String getFormIndex() {
        return formIndex;
    }

    @JsonSetter(value = "ix")
    public void setFormIndex(String formIndex) {
        this.formIndex = formIndex;
    }

    @JsonGetter(value = "answer")
    public Object getAnswer() {
        return answer;
    }

    @JsonSetter(value = "answer")
    public void setAnswer(Object answer) {
        this.answer = answer;
    }

    @Override
    public String toString() {
        return "Answer Question Bean [formIndex: " + formIndex + ", answer: " + answer
                + ", sessionId: " + sessionId + "]";
    }

    @JsonGetter(value = "nav_mode")
    public String getNavMode() {
        return navMode;
    }

    @JsonSetter(value = "nav_mode")
    public void setNavMode(String navMode) {
        this.navMode = navMode;
    }

    @Nullable
    public Map<String, Object> getAnswersToValidate() {
        return answersToValidate;
    }

    public void setAnswersToValidate(Map<String, Object> answersToValidate) {
        this.answersToValidate = answersToValidate;
    }
}
