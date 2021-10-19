package org.commcare.formplayer.session;

import com.fasterxml.jackson.databind.ObjectMapper;

import datadog.trace.api.Trace;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.formplayer.api.json.JsonActionUtils;
import org.commcare.formplayer.beans.FormEntryNavigationResponseBean;
import org.commcare.formplayer.beans.FormEntryResponseBean;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.objects.FormVolatilityRecord;
import org.commcare.formplayer.objects.FunctionHandler;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.serializer.FormDefStringSerializer;
import org.commcare.modern.database.TableBuilder;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.engine.CommCareConfigEngine;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.UnregisteredLocaleException;
import org.javarosa.engine.FunctionExtensions;
import org.javarosa.form.api.FormController;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xpath.XPathException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.Map;


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

    private final SerializableFormSession session;
    Log log = LogFactory.getLog(FormSession.class);

    private FormDef formDef;
    private FormEntryModel formEntryModel;
    private FormEntryController formEntryController;
    private FormController formController;
    private UserSqlSandbox sandbox;
    private String[] langs;
    private boolean isAtLastIndex = false;
    private boolean isAtFirstIndex;

    private FormVolatilityRecord sessionVolatilityRecord;
    private boolean shouldAutoSubmit;
    private boolean suppressAutosync;
    private boolean shouldSkipFullFormValidation;

    @Trace
    private void setupJavaRosaObjects() {
        formEntryModel = new FormEntryModel(formDef, FormEntryModel.REPEAT_STRUCTURE_NON_LINEAR);
        formEntryController = new FormEntryController(formEntryModel);
        formController = new FormController(formEntryController, false);
        langs = formEntryModel.getLanguages();
        initLocale();
    }

    public FormSession(SerializableFormSession session,
                       RestoreFactory restoreFactory,
                       FormSendCalloutHandler formSendCalloutHandler,
                       FormplayerStorageFactory storageFactory) throws Exception{

        this.session = session;
        //We don't want ongoing form sessions to change their db state underneath in the middle,
        //so suppress continuous syncs. Eventually this should likely go into the bean connector
        // for FormController endpoints rather than this config.
        restoreFactory.setPermitAggressiveSyncs(false);

        this.sandbox = restoreFactory.getSandbox();
        this.formDef = FormDefStringSerializer.deserialize(session.getFormXml());
        loadInstanceXml(formDef, session.getInstanceXml());
        formDef.setSendCalloutHandler(formSendCalloutHandler);
        setupJavaRosaObjects();

        if (session.isOneQuestionPerScreen() || session.isInPromptMode()) {
            FormIndex formIndex = JsonActionUtils.indexFromString(session.getCurrentIndex(), this.formDef);
            formController.jumpToIndex(formIndex);
            formEntryModel.setQuestionIndex(JsonActionUtils.indexFromString(session.getCurrentIndex(), formDef));
        }
        setupFunctionContext();
        initialize(false, session.getSessionData(), storageFactory.getStorageManager());
    }

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

        this.formDef = formDef;
        session = new SerializableFormSession(
                domain, appId, TableBuilder.scrubName(username), asUser, caseId,
                postUrl, menuSessionId, formDef.getTitle(), oneQuestionPerScreen,
                locale, inPromptMode, sessionData, functionContext
        );

        formDef.setSendCalloutHandler(formSendCalloutHandler);
        this.sandbox = sandbox;
        setupJavaRosaObjects();
        setupFunctionContext();
        if(instanceContent != null){
            loadInstanceXml(formDef, instanceContent);
            initialize(false, sessionData, storageFactory.getStorageManager());
        } else {
            initialize(true, sessionData, storageFactory.getStorageManager());
        }

        if (oneQuestionPerScreen) {
            stepToNextIndex();
            session.setCurrentIndex(formController.getFormIndex().toString());
        }
        // must be done after formDef is initialized
        session.setFormXml(FormDefStringSerializer.serialize(formDef));
    }

    /**
     * Setup static function handlers. At the moment we only expect/accept date functions
     * (in particular, now() and today()) but could be extended in the future.
     */
    @Trace
    private void setupFunctionContext() {
        if (session.getFunctionContext() == null || session.getFunctionContext().size() < 1) {
            return;
        }
        for (String outerKey: session.getFunctionContext().keySet()) {
            FunctionHandler[] functionHandlers = session.getFunctionContext().get(outerKey);
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

    @Trace
    private void loadInstanceXml(FormDef formDef, String instanceContent) throws IOException {
        StringReader stringReader = new StringReader(instanceContent);
        XFormParser xFormParser = new XFormParser(stringReader);
        xFormParser.loadXmlInstance(formDef, stringReader);
    }

    private void initLocale(){
        if(session.getInitLang() == null){
            session.setInitLang(this.langs[0]);
        }
        try {
            formEntryController.setLanguage(session.getInitLang());
        } catch (UnregisteredLocaleException e) {
            log.error("Couldn't find locale " + session.getInitLang()
                    + " for user " + session.getUsername());
        }
    }

    @Trace
    private void initialize(boolean newInstance, Map<String, String> sessionData, StorageManager storageManager) {
        CommCarePlatform platform = new CommCarePlatform(CommCareConfigEngine.MAJOR_VERSION,
                CommCareConfigEngine.MINOR_VERSION, CommCareConfigEngine.MINIMAL_VERSION, storageManager);
        FormplayerSessionWrapper sessionWrapper = new FormplayerSessionWrapper(platform, this.sandbox, sessionData);
        formDef.initialize(newInstance, sessionWrapper.getIIF(), session.getInitLang(), false);

        setVolatilityIndicators();
        setAutoSubmitFlag();
        setSuppressAutosyncFlag();
        setSkipValidation();
    }

    private String getPragma(String key) {
        String value = formDef.getLocalizer().getText(key);
        if(value != null) {
            return formDef.fillTemplateString(
                    value, TreeReference.rootRef());
        }
        return null;
    }

    /**
     * Volatility indicator are used to warn the current user if another user is already performing
     * the same action.
     */
    @Trace
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

    /**
     * When this flag is set the form will be automatically submitted by the Web Apps UI after
     * it has loaded. Assuming the form validation succeeds the form will be processed
     * without the need for user interaction.
     */
    private void setAutoSubmitFlag() {
        String shouldSubmit = getPragma("Pragma-Submit-Automatically");
        if(shouldSubmit != null ) {
            this.shouldAutoSubmit = true;
        }
        else {
            this.shouldAutoSubmit = false;
        }
    }

    /**
     * Disable auto-sync after form submissions for the current form session (if it was enabled).
     * This is useful  when it is combined "Pragma-Submit-Automatically" so that multiple automatic submissions
     * can be done without the need for sync in between each one.
     * See {@link org.commcare.formplayer.util.FormplayerPropertyManager#POST_FORM_SYNC}
     */
    private void setSuppressAutosyncFlag() {
        String shouldSubmit = getPragma("Pragma-Suppress-Autosync");
        if(shouldSubmit != null ) {
            this.suppressAutosync = true;
        }
        else {
            this.suppressAutosync = false;
        }
    }

    /**
     * This allows forms to skip some validation that occurs on submit.
     * If the answer in the submission matches the answer in the model, it will no revalidate.
     * This will still catch required questions and changes since the last validation,
     * but will no longer catch the case where a later response invalidates an earlier one.
     * As such, it should be used with caution, but will provide meaningful speed-ups when used in that way.
     */
    private void setSkipValidation() {
        String shouldSkipValidation = getPragma("Pragma-Skip-Full-Form-Validation");
        if (shouldSkipValidation != null) {
            this.shouldSkipFullFormValidation = true;
        } else {
            this.shouldSkipFullFormValidation = false;
        }
    }

    public boolean getAutoSubmitFlag() {
        return shouldAutoSubmit;
    }

    public boolean getSuppressAutosync() {
        return suppressAutosync;
    }

    public boolean getSkipValidation() {
        return shouldSkipFullFormValidation;
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

    public String[] getLanguages(){
        return langs;
    }

    @Trace
    public JSONArray getFormTree() {
        if (session.isOneQuestionPerScreen()) {
            return JsonActionUtils.getOneQuestionPerScreenJSON(formController.getFormEntryController().getModel(),
                    formController.getFormEntryController(),
                    formController.getFormIndex());
        }
        return JsonActionUtils.getFullFormJSON(getFormEntryModel(), getFormEntryController());
    }


    public String getSessionId(){
        return session.getId();
    }

    public String getXmlns(){
        Object metaData = getFormEntryModel().getForm().getMainInstance().getMetaData(FormInstance.META_XMLNS);
        if(metaData == null){
            return null;
        }
        return metaData.toString();
    }

    public String getLocale() {
        return session.getInitLang();
    }

    public UserSandbox getSandbox(){
        return this.sandbox;
    }


    public String submitGetXml() throws IOException {
        formDef.postProcessInstance();
        return getInstanceXml();
    }

    public SerializableFormSession serialize() throws IOException {
        session.setInstanceXml(getInstanceXml());
        return session;
    }

    public String getPostUrl() {
        return session.getPostUrl();
    }

    public String getUsername(){
        return session.getUsername();
    }

    public String getMenuSessionId() {
        return session.getMenuSessionId();
    }

    public void setIsAtLastIndex(boolean isAtLastIndex) {
        this.isAtLastIndex = isAtLastIndex;
    }

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
            session.setCurrentIndex(formController.getFormIndex().toString());
        }
    }

    public void stepToPreviousIndex() {
        moveToPreviousView();
        session.setCurrentIndex(formController.getFormIndex().toString());
    }

    public FormEntryResponseBean getCurrentJSON() throws IOException {
        JSONObject jsonObject = JsonActionUtils.getCurrentJson(formEntryController, formEntryModel);
        return new ObjectMapper().readValue(jsonObject.toString(), FormEntryResponseBean.class);
    }

    @Trace
    public FormEntryResponseBean answerQuestionToJSON(Object answer, String answerIndex) throws IOException {
        if (answerIndex == null) {
            answerIndex = session.getCurrentIndex();
        }

        JSONObject jsonObject = JsonActionUtils.questionAnswerToJson(formEntryController,
                formEntryModel,
                answer != null ? answer.toString() : null,
                answerIndex,
                session.isOneQuestionPerScreen(),
                session.getCurrentIndex(),
                false,
                true);

        FormEntryResponseBean response = new ObjectMapper().readValue(jsonObject.toString(), FormEntryResponseBean.class);
        if (!session.isInPromptMode() || !Constants.ANSWER_RESPONSE_STATUS_POSITIVE.equals(response.getStatus())) {
            return response;
        }
        return getNextFormNavigation();
    }

    public FormEntryNavigationResponseBean getFormNavigation() throws IOException {
        JSONObject resp = JsonActionUtils.getCurrentJson(formEntryController, formEntryModel, session.getCurrentIndex());
        FormEntryNavigationResponseBean responseBean
                = new ObjectMapper().readValue(resp.toString(), FormEntryNavigationResponseBean.class);
        responseBean.setIsAtLastIndex(isAtLastIndex);
        responseBean.setIsAtFirstIndex(isAtFirstIndex);
        responseBean.setTitle(session.getTitle());
        responseBean.setCurrentIndex(session.getCurrentIndex());
        responseBean.setEvent(responseBean.getTree()[0]);
        return responseBean;
    }

    /**
     *  Automatically advance to the next question after an answer when in "prompt" mode
     *  (currently only used by SMS)
     */
    public FormEntryNavigationResponseBean getNextFormNavigation() throws IOException {
        formEntryModel.setQuestionIndex(JsonActionUtils.indexFromString(session.getCurrentIndex(), formDef));
        int nextEvent = formEntryController.stepToNextEvent();
        session.setCurrentIndex(formController.getFormIndex().toString());
        JSONObject resp = JsonActionUtils.getPromptJson(formEntryModel);
        FormEntryNavigationResponseBean responseBean
                = new ObjectMapper().readValue(resp.toString(), FormEntryNavigationResponseBean.class);
        responseBean.setIsAtLastIndex(isAtLastIndex);
        responseBean.setIsAtFirstIndex(isAtFirstIndex);
        responseBean.setTitle(session.getTitle());
        responseBean.setCurrentIndex(session.getCurrentIndex());
        responseBean.setInstanceXml(null);
        responseBean.setTree(null);
        responseBean.setStatus(Constants.ANSWER_RESPONSE_STATUS_POSITIVE);
        if (nextEvent == formEntryController.EVENT_END_OF_FORM) {
            String output = submitGetXml();
            responseBean.getEvent().setOutput(output);
        }
        return responseBean;
    }

    public void setIsAtFirstIndex(boolean isAtFirstIndex) {
        this.isAtFirstIndex = isAtFirstIndex;
    }

    public void changeLocale(String locale) {
        session.setInitLang(locale);
        formEntryController.setLanguage(locale);
    }

    public SerializableFormSession getSerializableSession() {
        return session;
    }
}
