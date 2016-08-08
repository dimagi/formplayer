package util;

/**
 * Created by willpride on 2/4/16.
 */
public class Constants {
    //URLS
    public final static String URL_NEW_SESSION = "new-form";
    public final static String URL_INCOMPLETE_SESSION = "incomplete-form";
    public final static String URL_ANSWER_QUESTION = "answer";
    public final static String URL_CURRENT = "current";
    public final static String URL_SUBMIT_FORM = "submit-all";
    public final static String URL_GET_INSTANCE = "get-instance";
    public final static String URL_EVALUATE_XPATH = "evaluate-xpath";
    public final static String URL_NEW_REPEAT = "new-repeat";
    public final static String URL_DELETE_REPEAT = "delete-repeat";
    public final static String URL_FILTER_CASES = "filter_cases";
    public final static String URL_FILTER_CASES_FULL = "filter_cases_full";
    public final static String URL_SYNC_DB = "sync-db";
    public final static String URL_LIST_SESSIONS = "sessions";
    public final static String URL_GET_SESSION = "get_session";
    public static final String URL_INSTALL = "install";
    public static final String URL_MENU_NAVIGATION = "navigate_menu";
    public static final String URL_GET_SESSIONS = "get_sessions";
    public static final String URL_SERVER_UP = "serverup";
    public static final String URL_GIT_STATUS = "git_status";
    //Menus
    public static final String MENU_MODULE = "modules";
    public static final String MENU_ENTITY = "entity";
    public static final String CASE_LIST_ACTION = "action";
    //Status
    public static final String ANSWER_RESPONSE_STATUS_POSITIVE = "accepted";
    public static final String ANSWER_RESPONSE_STATUS_NEGATIVE = "validation-error";
    public static final String SYNC_RESPONSE_STATUS_POSITIVE = "success";
    //JSON
    public static final String QUESTION_TREE_KEY = "tree";

    // Postgres sesions
    public static final String POSTGRES_SESSION_TABLE_NAME = "formplayer_sessions";
    public static final String POSTGRES_TOKEN_TABLE_NAME = "django_session";
    public static final String POSTGRES_MENU_SESSION_TABLE_NAME = "menu_sessions";

    public static final String POSTGRES_DJANGO_SESSION_ID = "sessionid";
}
