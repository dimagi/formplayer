package org.commcare.formplayer.beans;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.commcare.formplayer.beans.menus.EntityDetailResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Created by willpride on 1/12/16.
 */
public class NewFormResponse extends SessionResponseBean {
    private QuestionBean[] tree;
    private String[] langs;
    private String[] breadcrumbs;
    private EntityDetailResponse persistentCaseTile;
    private QuestionBean event;
    private Map<String, String> webAppsPragmas;

    public NewFormResponse() {
    }

    public NewFormResponse(String formTreeJson, String[] languages,
            String title, String sessionId, int sequenceId,
            String instanceXml) throws IOException {
        this.tree = new ObjectMapper().readValue(formTreeJson, QuestionBean[].class);
        this.langs = languages;
        this.title = title;
        this.sessionId = sessionId;
        this.sequenceId = sequenceId;
        this.instanceXml = new InstanceXmlBean(instanceXml);
    }

    public QuestionBean[] getTree() {
        return tree;
    }

    public String[] getLangs() {
        return langs;
    }

    public String getSession_id() {
        return sessionId;
    }

    public String toString() {
        return "NewFormResponse [sessionId=" + sessionId + ", title=" + title + "]";
    }

    public String[] getBreadcrumbs() {
        return breadcrumbs;
    }

    public void setBreadcrumbs(String[] breadcrumbs) {
        this.breadcrumbs = breadcrumbs;
    }

    public EntityDetailResponse getPersistentCaseTile() {
        return persistentCaseTile;
    }

    public void setPersistentCaseTile(EntityDetailResponse persistentCaseTile) {
        this.persistentCaseTile = persistentCaseTile;
    }

    public QuestionBean getEvent() {
        return event;
    }

    public void setEvent(QuestionBean event) {
        this.event = event;
    }

    public void setTree(QuestionBean[] tree) {
        this.tree = tree;
    }

    public Map<String, String> getWebAppsPragmas() {
        return webAppsPragmas;
    }

    public void setWebAppsPragmas(Map<String, String> webAppsPragmas) {
        this.webAppsPragmas = webAppsPragmas;
    }
}
