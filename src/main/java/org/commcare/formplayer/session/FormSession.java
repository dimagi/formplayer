package org.commcare.formplayer.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import datadog.trace.api.Trace;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.modern.database.TableBuilder;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.engine.CommCareConfigEngine;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.GroupDef;
import org.javarosa.core.model.IFormElement;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.UnregisteredLocaleException;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.engine.FunctionExtensions;
import org.javarosa.form.api.FormController;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.schema.FormInstanceLoader;
import org.javarosa.xpath.XPathException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.commcare.formplayer.api.json.JsonActionUtils;
import org.commcare.formplayer.beans.FormEntryNavigationResponseBean;
import org.commcare.formplayer.beans.FormEntryResponseBean;
import org.commcare.formplayer.objects.FormVolatilityRecord;
import org.commcare.formplayer.objects.FunctionHandler;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.util.Constants;

/**
 *
 * OK this (and MenuSession) is a total god object that basically mananges everything about the state of
 * a form entry session. We turn this into a SerializableFormSession to persist it. Within that we also
 * serialize the formDef to persist the session, in addition to a bunch of other information.
 * Confusingly we also have a SessionWrapper object within this session which tracks a bunch of other information. There
 * is a lot of unification that needs to happen here.
 *
 * @author willpride
 */
public class FormSession {

    Log log = LogFactory.getLog(FormSession.class);

    private FormDef formDef;
    private FormEntryModel formEntryModel;
    private FormEntryController formEntryController;
    private FormController formController;
    private UserSqlSandbox sandbox;
    private int sequenceId;
    private String dateOpened;
    private String locale;
    private Map<String, String> sessionData;
    private String postUrl;
    private String title;
    private String[] langs;
    private String uuid;
    private final String username;
    private String domain;
    private String menuSessionId;
    private boolean oneQuestionPerScreen;
    private String currentIndex = "-1";
    private boolean isAtLastIndex = false;
    private String asUser;
    private String appId;
    private Map<String, FunctionHandler[]> functionContext;
    private boolean isAtFirstIndex;
    private boolean inPromptMode;
    private String restoreAsCaseId;

    private FormVolatilityRecord sessionVolatilityRecord;

    private void setupJavaRosaObjects() {
        formEntryModel = new FormEntryModel(formDef, FormEntryModel.REPEAT_STRUCTURE_NON_LINEAR);
        formEntryController = new FormEntryController(formEntryModel);
        formController = new FormController(formEntryController, false);
        title = formDef.getTitle();
        langs = formEntryModel.getLanguages();
        initLocale();
    }

    // Session object for ongoing sessions
    public FormSession(SerializableFormSession session,
                       RestoreFactory restoreFactory,
                       FormSendCalloutHandler formSendCalloutHandler,
                       FormplayerStorageFactory storageFactory) throws Exception{

        //We don't want ongoing form sessions to change their db state underneath in the middle,
        //so suppress continuous syncs. Eventually this should likely go into the bean connector
        // for FormController endpoints rather than this config.
        restoreFactory.setPermitAggressiveSyncs(false);

        this.username = session.getUsername();
        this.asUser = session.getAsUser();
        this.appId = session.getAppId();
        this.domain = session.getDomain();
        this.sandbox = restoreFactory.getSandbox();
        this.postUrl = session.getPostUrl();
        this.sessionData = session.getSessionData();
        this.oneQuestionPerScreen = session.getOneQuestionPerScreen();
        this.locale = session.getInitLang();
        this.currentIndex = session.getCurrentIndex();
        this.uuid = session.getId();
        this.sequenceId = session.getSequenceId();
        this.menuSessionId = session.getMenuSessionId();
        this.dateOpened = session.getDateOpened();
        this.formDef = new FormDef();
        deserializeFormDef(session.getFormXml());
        this.formDef = FormInstanceLoader.loadInstance(formDef, IOUtils.toInputStream(session.getInstanceXml()));
        this.inPromptMode = session.getInPromptMode();
        formDef.setSendCalloutHandler(formSendCalloutHandler);
        this.functionContext = session.getFunctionContext();
        this.restoreAsCaseId = session.getRestoreAsCaseId();
        setupJavaRosaObjects();
        if (this.oneQuestionPerScreen || this.inPromptMode) {
            FormIndex formIndex = JsonActionUtils.indexFromString(currentIndex, this.formDef);
            formController.jumpToIndex(formIndex);
            formEntryModel.setQuestionIndex(JsonActionUtils.indexFromString(this.currentIndex, formDef));
        }
        setupFunctionContext();
        initialize(false, session.getSessionData(), storageFactory.getStorageManager());
        this.postUrl = session.getPostUrl();
    }

    // New FormSession constructor
    public FormSession(UserSqlSandbox sandbox,
                       FormDef formDef,
                       String username,
                       String domain,
                       Map<String, String> sessionData,
                       String postUrl,
                       String locale,
                       String menuSessionId,
                       String instanceContent,
                       boolean oneQuestionPerScreen,
                       String asUser,
                       String appId,
                       Map<String, FunctionHandler[]> functionContext,
                       FormSendCalloutHandler formSendCalloutHandler,
                       FormplayerStorageFactory storageFactory,
                       boolean inPromptMode,
                       String caseId) throws Exception {
        this.username = TableBuilder.scrubName(username);
        this.formDef = formDef;
        formDef.setSendCalloutHandler(formSendCalloutHandler);
        this.sandbox = sandbox;
        this.sessionData = sessionData;
        this.domain = domain;
        this.postUrl = postUrl;
        this.locale = locale;
        this.uuid = UUID.randomUUID().toString();
        this.sequenceId = 0;
        this.postUrl = postUrl;
        this.menuSessionId = menuSessionId;
        this.oneQuestionPerScreen = oneQuestionPerScreen;
        this.asUser = asUser;
        this.appId = appId;
        this.currentIndex = "0";
        this.functionContext = functionContext;
        this.inPromptMode = inPromptMode;
        this.restoreAsCaseId = caseId;
        setupJavaRosaObjects();
        setupFunctionContext();
        if(instanceContent != null){
            loadInstanceXml(formDef, instanceContent);
            initialize(false, sessionData, storageFactory.getStorageManager());
        } else {
            initialize(true, sessionData, storageFactory.getStorageManager());
        }
        
        if (this.oneQuestionPerScreen) {
            stepToNextIndex();
            this.currentIndex = formController.getFormIndex().toString();
        }
    }

    /**
     * Setup static function handlers. At the moment we only expect/accept date functions
     * (in particular, now() and today()) but could be extended in the future.
     */
    private void setupFunctionContext() {
        if (functionContext == null || functionContext.size() < 1) {
            return;
        }
        for (String outerKey: functionContext.keySet()) {
            FunctionHandler[] functionHandlers = functionContext.get(outerKey);
            if(outerKey.equals("static-date")) {
                for (FunctionHandler functionHandler: functionHandlers) {
                    String funcName = functionHandler.getName();
                    Date funcValue;
                    if (funcName.contains("now")) {
                        funcValue = DateUtils.parseDateTime(functionHandler.getValue());
                    } else {
                        funcValue = DateUtils.parseDate(functionHandler.getValue());
                    }
                    formDef.exprEvalContext.addFunctionHandler(
                        new FunctionExtensions.TodayFunc(
                                funcName,
                                funcValue)
                    );
                }
            }
        }
    }

    private void loadInstanceXml(FormDef formDef, String instanceContent) throws IOException {
        StringReader stringReader = new StringReader(instanceContent);
        XFormParser xFormParser = new XFormParser(stringReader);
        xFormParser.loadXmlInstance(formDef, stringReader);
    }

    private void initLocale(){
        if(locale == null){
            this.locale = this.langs[0];
        }
        try {
            formEntryController.setLanguage(this.locale);
        } catch (UnregisteredLocaleException e) {
            log.error("Couldn't find locale " + this.locale
                    + " for user " + username);
        }
    }

    @Trace
    private void initialize(boolean newInstance, Map<String, String> sessionData, StorageManager storageManager) {
        CommCarePlatform platform = new CommCarePlatform(CommCareConfigEngine.MAJOR_VERSION,
                CommCareConfigEngine.MINOR_VERSION, storageManager);
        FormplayerSessionWrapper sessionWrapper = new FormplayerSessionWrapper(platform, this.sandbox, sessionData);
        formDef.initialize(newInstance, sessionWrapper.getIIF(), locale, false);

        setVolatilityIndicators();
    }

    private String getPragma(String key) {
        String value = formDef.getLocalizer().getText(key);
        if(value != null) {
            return formDef.fillTemplateString(
                    value, TreeReference.rootRef());
        }
        return null;
    }

    private void setVolatilityIndicators()
    {
        String volatilityKey = getPragma("Pragma-Volatility-Key");
        String entityTitle = getPragma("Pragma-Volatility-Entity-Title");

        if(volatilityKey != null) {
            this.sessionVolatilityRecord = new FormVolatilityRecord(
                    String.format(FormVolatilityRecord.VOLATILITY_KEY_TEMPLATE,
                            this.getXmlns(),
                            volatilityKey),
                    this.getVolatilityKeyTimeout(),
                    entityTitle);
        }
    }

    public FormVolatilityRecord getSessionVolatilityRecord() {
        return sessionVolatilityRecord;
    }

    /**
     * @return the Timeout (in seconds) for volatility notices for this form
     */
    private long getVolatilityKeyTimeout() {
        String timeOut = getPragma("Pragma-Volatility-Window");

        int timeOutWindow = 5 * 60;

        if (timeOut != null) {
            try {
                int timeOutInput = Integer.parseInt(timeOut);
                timeOutWindow = timeOutInput;
            } catch (NumberFormatException nfe) {
                System.out.println("Invalid timeout window: " + timeOut);
            }
        }
        return timeOutWindow;
    }

    @Trace
    public String getInstanceXml() throws IOException {
        byte[] bytes = new XFormSerializingVisitor().serializeInstance(formDef.getInstance());
        return new String(bytes, "US-ASCII");
    }

    public FormEntryModel getFormEntryModel(){
        return formEntryModel;
    }

    public FormEntryController getFormEntryController(){
        return formEntryController;
    }

    public String getTitle(){
        return title;
    }

    public String[] getLanguages(){
        return langs;
    }

    public JSONArray getFormTree() {
        if (oneQuestionPerScreen) {
            return JsonActionUtils.getOneQuestionPerScreenJSON(formController.getFormEntryController().getModel(),
                    formController.getFormEntryController(),
                    formController.getFormIndex());
        }
        return JsonActionUtils.getFullFormJSON(getFormEntryModel(), getFormEntryController());
    }


    public String getSessionId(){
        return uuid;
    }

    public String getXmlns(){
        Object metaData = getFormEntryModel().getForm().getMainInstance().getMetaData(FormInstance.META_XMLNS);
        if(metaData == null){
            return null;
        }
        return metaData.toString();
    }

    public int getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(int sequenceId) {
        this.sequenceId = sequenceId;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public UserSandbox getSandbox(){
        return this.sandbox;
    }


    public String submitGetXml() throws IOException {
        formDef.postProcessInstance();
        return getInstanceXml();
    }

    public Map<String, String> getSessionData(){
        return sessionData;
    }

    private String serializeFormDef() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream serializedStream = new DataOutputStream(baos);
        formDef.writeExternal(serializedStream);
        return Base64.encodeBase64String(baos.toByteArray());
    }

    private void deserializeFormDef(String serializedFormDef) throws IOException, DeserializationException {
        byte [] sessionBytes = Base64.decodeBase64(serializedFormDef);
        DataInputStream inputStream =
                new DataInputStream(new ByteArrayInputStream(sessionBytes));
        formDef.readExternal(inputStream, PrototypeManager.getDefault());
    }


    public SerializableFormSession serialize() throws IOException {
        SerializableFormSession serializableFormSession = new SerializableFormSession();
        serializableFormSession.setInstanceXml(getInstanceXml());
        serializableFormSession.setId(getSessionId());
        serializableFormSession.setFormXml(serializeFormDef());
        serializableFormSession.setUsername(username);
        serializableFormSession.setSessionData(getSessionData());
        serializableFormSession.setSequenceId(getSequenceId());
        serializableFormSession.setInitLang(getLocale());
        serializableFormSession.setSessionData(getSessionData());
        serializableFormSession.setDomain(getDomain());
        serializableFormSession.setPostUrl(getPostUrl());
        serializableFormSession.setMenuSessionId(menuSessionId);
        serializableFormSession.setTitle(getTitle());
        serializableFormSession.setDateOpened(new Date().toString());
        serializableFormSession.setOneQuestionPerScreen(oneQuestionPerScreen);
        serializableFormSession.setCurrentIndex(currentIndex);
        serializableFormSession.setAsUser(asUser);
        serializableFormSession.setAppId(appId);
        serializableFormSession.setFunctionContext(functionContext);
        serializableFormSession.setInPromptMode(inPromptMode);
        serializableFormSession.setRestoreAsCaseId(restoreAsCaseId);
        return serializableFormSession;
    }

    public String getDomain() {
        return domain;
    }

    public String getPostUrl() {
        return postUrl;
    }

    public String getUsername(){
        return username;
    }

    public String getMenuSessionId() {
        return menuSessionId;
    }

    public void setCurrentIndex(String index) {
        this.currentIndex = index;
    }

    public String getCurrentIndex() {
        return currentIndex;
    }

    public void setIsAtLastIndex(boolean isAtLastIndex) {
        this.isAtLastIndex = isAtLastIndex;
    }

    public boolean getIsAtLastIndex() {
        return isAtLastIndex;
    }

    public FormDef getFormDef() { return formDef; }

    private void moveToNextView() {
        if (formController.getEvent() != FormEntryController.EVENT_END_OF_FORM) {
            int event;

            try {
                group_skip:
                do {
                    event = formController.stepToNextEvent(FormEntryController.STEP_OVER_GROUP);
                    switch (event) {
                        case FormEntryController.EVENT_QUESTION:
                            break group_skip;
                        case FormEntryController.EVENT_END_OF_FORM:
                            break group_skip;
                        case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
                            break group_skip;
                        case FormEntryController.EVENT_GROUP:
                            //We only hit this event if we're at the _opening_ of a field
                            //list, so it seems totally fine to do it this way, technically
                            //though this should test whether the index is the field list
                            //host.
                            if (formController.indexIsInFieldList()
                                    && formController.getQuestionPrompts().length != 0) {
                                break group_skip;
                            }
                            // otherwise it's not a field-list group, so just skip it
                            break;
                        case FormEntryController.EVENT_REPEAT:
                            // skip repeats
                            break;
                        case FormEntryController.EVENT_REPEAT_JUNCTURE:
                            // skip repeat junctures until we implement them
                            break;
                        default:
                            break;
                    }
                } while (event != FormEntryController.EVENT_END_OF_FORM);
            } catch (XPathException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Determines what should be displayed between a question, or the start screen and displays the
     * appropriate view. Also saves answers to the data model without checking constraints.
     */
    protected void moveToPreviousView() {

        FormIndex startIndex = formController.getFormIndex();
        FormIndex lastValidIndex = startIndex;

        if (formController.getEvent() != FormEntryController.EVENT_BEGINNING_OF_FORM) {
            int event = formController.stepToPreviousEvent();

            //Step backwards until we either find a question, the beginning of the form,
            //or a field list with valid questions inside
            while (event != FormEntryController.EVENT_BEGINNING_OF_FORM
                    && event != FormEntryController.EVENT_QUESTION
                    && !(event == FormEntryController.EVENT_GROUP
                    && formController.indexIsInFieldList() && formController.getQuestionPrompts().length != 0)) {
                event = formController.stepToPreviousEvent();
                lastValidIndex = formController.getFormIndex();
            }

            if (event == FormEntryController.EVENT_BEGINNING_OF_FORM) {
                // we can't go all the way back to the beginning, so we've
                // gotta hit the last index that was valid
                formController.jumpToIndex(lastValidIndex);
                setIsAtFirstIndex(true);

                if (lastValidIndex.isBeginningOfFormIndex()) {
                    //We might have walked all the way back still, which isn't great,
                    //so keep moving forward again until we find it
                    //there must be a repeat between where we started and the beginning of hte form, walk back up to it
                    moveToNextView();
                }
            }
        }
    }

    public void stepToNextIndex() {
        moveToNextView();
        int event = formController.getEvent();
        boolean isEndOfForm = event == FormEntryController.EVENT_END_OF_FORM;
        setIsAtLastIndex(isEndOfForm);
        if (!isEndOfForm) {
            setCurrentIndex(formController.getFormIndex().toString());
        }
    }

    public void stepToPreviousIndex() {
        moveToPreviousView();
        int event = formController.getEvent();
        setCurrentIndex(formController.getFormIndex().toString());
    }

    public FormEntryResponseBean getCurrentJSON() throws IOException {
        JSONObject jsonObject = JsonActionUtils.getCurrentJson(formEntryController, formEntryModel);
        return new ObjectMapper().readValue(jsonObject.toString(), FormEntryResponseBean.class);
    }

    public FormEntryResponseBean answerQuestionToJSON(Object answer, String answerIndex) throws IOException {
        if (answerIndex == null) {
            answerIndex = getCurrentIndex();
        }

        JSONObject jsonObject = JsonActionUtils.questionAnswerToJson(formEntryController,
                formEntryModel,
                answer != null ? answer.toString() : null,
                answerIndex,
                oneQuestionPerScreen,
                currentIndex);

        FormEntryResponseBean response = new ObjectMapper().readValue(jsonObject.toString(), FormEntryResponseBean.class);
        if (!inPromptMode || !Constants.ANSWER_RESPONSE_STATUS_POSITIVE.equals(response.getStatus())) {
            return response;
        }
        return getNextFormNavigation();
    }

    public FormEntryNavigationResponseBean getFormNavigation() throws IOException {
        JSONObject resp = JsonActionUtils.getCurrentJson(formEntryController, formEntryModel, currentIndex);
        FormEntryNavigationResponseBean responseBean
                = new ObjectMapper().readValue(resp.toString(), FormEntryNavigationResponseBean.class);
        responseBean.setIsAtLastIndex(isAtLastIndex);
        responseBean.setIsAtFirstIndex(isAtFirstIndex);
        responseBean.setTitle(title);
        responseBean.setCurrentIndex(currentIndex);
        responseBean.setEvent(responseBean.getTree()[0]);
        return responseBean;
    }

    /**
     *  Automatically advance to the next question after an answer when in "prompt" mode
     *  (currently only used by SMS)
     */
    public FormEntryNavigationResponseBean getNextFormNavigation() throws IOException {
        formEntryModel.setQuestionIndex(JsonActionUtils.indexFromString(this.currentIndex, formDef));
        int nextEvent = formEntryController.stepToNextEvent();
        this.currentIndex = formController.getFormIndex().toString();
        JSONObject resp = JsonActionUtils.getPromptJson(formEntryModel);
        FormEntryNavigationResponseBean responseBean
                = new ObjectMapper().readValue(resp.toString(), FormEntryNavigationResponseBean.class);
        responseBean.setIsAtLastIndex(isAtLastIndex);
        responseBean.setIsAtFirstIndex(isAtFirstIndex);
        responseBean.setTitle(title);
        responseBean.setCurrentIndex(currentIndex);
        responseBean.setInstanceXml(null);
        responseBean.setTree(null);
        responseBean.setStatus(Constants.ANSWER_RESPONSE_STATUS_POSITIVE);
        if (nextEvent == formEntryController.EVENT_END_OF_FORM) {
            String output = submitGetXml();
            responseBean.getEvent().setOutput(output);
        }
        return responseBean;
    }

    public String getDateOpened() {
        return dateOpened;
    }

    public void setDateOpened(String dateOpened) {
        this.dateOpened = dateOpened;
    }

    public String getAsUser() {
        return asUser;
    }

    public void setAsUser(String asUser) {
        this.asUser = asUser;
    }

    public boolean getOneQuestionPerScreen() {
        return oneQuestionPerScreen;
    }

    public void reload(FormDef formDef, String postUrl, StorageManager storageManager) throws IOException {
        if(getInstanceXml() != null){
            loadInstanceXml(formDef, getInstanceXml());
            initialize(false, sessionData, storageManager);
        } else {
            initialize(true, sessionData, storageManager);
        }
        if (this.oneQuestionPerScreen) {
            FormIndex firstIndex = JsonActionUtils.indexFromString(currentIndex, this.formDef);
            IFormElement element = formEntryController.getModel().getForm().getChild(firstIndex);
            while (element instanceof GroupDef && !formEntryController.isFieldListHost(firstIndex)) {
                firstIndex =  formController.getNextFormIndex(firstIndex, false, true);
                element = formEntryController.getModel().getForm().getChild(firstIndex);
            }
            this.currentIndex = firstIndex.toString();
        }
        this.postUrl = postUrl;
    }

    public void setIsAtFirstIndex(boolean isAtFirstIndex) {
        this.isAtFirstIndex = isAtFirstIndex;
    }

    public void changeLocale(String locale) {
        this.locale = locale;
        formEntryController.setLanguage(locale);
    }
}
