package util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by willpride on 2/4/16.
 */
public class Constants {
    //URLS
    public final static String URL_NEW_SESSION = "new-form";
    public final static String URL_INCOMPLETE_SESSION = "incomplete-form";
    public final static String URL_DELETE_INCOMPLETE_SESSION = "delete-incomplete-form";
    public final static String URL_ANSWER_QUESTION = "answer";
    public final static String URL_CURRENT = "current";
    public final static String URL_SUBMIT_FORM = "submit-all";
    public final static String URL_EVALUATE_XPATH = "evaluate-xpath";
    public final static String URL_NEW_REPEAT = "new-repeat";
    public final static String URL_DELETE_REPEAT = "delete-repeat";
    public final static String URL_SYNC_DB = "sync-db";
    public final static String URL_LIST_SESSIONS = "sessions";
    public final static String URL_GET_SESSION = "get_session";
    public static final String URL_INSTALL = "install";
    public static final String URL_UPDATE = "update";
    public static final String URL_MENU_NAVIGATION = "navigate_menu";
    public static final String URL_GET_DETAILS = "get_details";
    public static final String URL_GET_SESSIONS = "get_sessions";
    public static final String URL_SERVER_UP = "serverup";
    public static final String URL_PREVIEW_FORM = "preview_form";
    public static final String URL_DELETE_APPLICATION_DBS = "delete_application_dbs";
    public static final String URL_NEXT_INDEX = "next_index";
    public static final String URL_PREV_INDEX = "prev_index";
    public static final String URL_VALIDATE_FORM = "validate_form";
    // Debugger URLS
    public static final String URL_DEBUGGER_FORMATTED_QUESTIONS = "formatted_questions";

    // Change this version when a backwards incompatible change is made to the
    // mobile sqlite dbs.
    public static final String SQLITE_DB_VERSION = "V1";

    //Menus
    public static final String MENU_MODULE = "modules";
    public static final String MENU_ENTITY = "entity";
    public static final String CASE_LIST_ACTION = "action";
    //Status
    public static final String ANSWER_RESPONSE_STATUS_POSITIVE = "accepted";
    public static final String ANSWER_RESPONSE_STATUS_NEGATIVE = "validation-error";
    public static final String SYNC_RESPONSE_STATUS_POSITIVE = "success";

    // Error return types
    public static final String ERROR_TYPE_TEXT = "text";
    public static final String ERROR_TYPE_HTML = "html";

    // Error status types
    public static final String ERROR_STATUS = "error";
    public static final String RETRY_STATUS = "retry";

    //JSON
    public static final String QUESTION_TREE_KEY = "tree";

    // CCZ Parameters
    public static final String CCZ_LATEST_SAVED = "save";

    // Postgres tables
    public static final String POSTGRES_SESSION_TABLE_NAME = "formplayer_sessions";
    public static final String POSTGRES_TOKEN_TABLE_NAME = "django_session";
    // Token table generated from django rest framework
    public static final String POSTGRES_AUTH_TOKEN_TABLE_NAME = "authtoken_token";
    public static final String POSTGRES_USER_TABLE_NAME = "auth_user";
    public static final String POSTGRES_MENU_SESSION_TABLE_NAME = "menu_sessions";

    // Couch databases
    public static final String COUCH_USERS_DB = "__users";

    public static final String POSTGRES_DJANGO_SESSION_ID = "sessionid";
    public static final String ANONYMOUS_USERNAME = "anonymous_user";
    public static final int ANONYMOUS_DJANGO_USERID = -1;
    public static final String COMMCARE_USER_SUFFIX = "commcarehq.org";

    public static final Set<Pattern> AUTH_WHITELIST = new HashSet<Pattern>(Arrays.asList(
            Pattern.compile(Constants.URL_SERVER_UP),
            Pattern.compile(Constants.URL_VALIDATE_FORM),
            Pattern.compile("swagger.*"),
            Pattern.compile("webjars/.*"),
            Pattern.compile("configuration/.*"),
            Pattern.compile("v2/.*")
    ));

    public static final int USER_LOCK_TIMEOUT = 120;
    // 15 minutes in milliseconds
    public static final int LOCK_DURATION = 60 * 15 * 1000;

    // Datadog metrics
    public static final String DATADOG_REQUESTS = "requests";
    public static final String DATADOG_TIMINGS = "timings";

    // Errors
    public static final String DATADOG_ERRORS_APP_CONFIG = "errors.app_config";
    public static final String DATADOG_ERRORS_EXTERNAL_REQUEST = "errors.external_request";
    public static final String DATADOG_ERRORS_CRASH = "errors.crash";
    // End Datadog metrics


}
