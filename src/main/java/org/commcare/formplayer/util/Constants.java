package org.commcare.formplayer.util;

/**
 * Created by willpride on 2/4/16.
 */
public class Constants {
    //URLS
    public static final String URL_NEW_SESSION = "new-form";
    public static final String URL_INCOMPLETE_SESSION = "incomplete-form";
    public static final String URL_DELETE_INCOMPLETE_SESSION = "delete-incomplete-form";
    public static final String URL_ANSWER_QUESTION = "answer";
    public static final String URL_CLEAR_ANSWER = "clear_answer";
    public static final String URL_ANSWER_MEDIA_QUESTION = "answer_media";
    public static final String URL_CURRENT = "current";
    public static final String URL_SUBMIT_FORM = "submit-all";
    public static final String URL_EVALUATE_XPATH = "evaluate-xpath";
    public static final String URL_EVALUATE_MENU_XPATH = "evaluate-menu-xpath";
    public static final String URL_NEW_REPEAT = "new-repeat";
    public static final String URL_DELETE_REPEAT = "delete-repeat";
    public static final String URL_SYNC_DB = "sync-db";
    public static final String URL_INTERVAL_SYNC_DB = "interval_sync-db";
    public static final String URL_LIST_SESSIONS = "sessions";
    public static final String URL_GET_SESSION = "get_session";
    public static final String URL_UPDATE = "update";
    public static final String URL_INITIAL_MENU_NAVIGATION = "navigate_menu_start";
    public static final String URL_MENU_NAVIGATION = "navigate_menu";
    public static final String URL_GET_ENDPOINT = "get_endpoint";
    public static final String URL_GET_DETAILS = "get_details";
    public static final String URL_GET_SESSIONS = "get_sessions";
    public static final String URL_SERVER_UP = "serverup";
    public static final String URL_PREVIEW_FORM = "preview_form";
    public static final String URL_DELETE_APPLICATION_DBS = "delete_application_dbs";
    public static final String URL_CLEAR_USER_DATA = "clear_user_data";
    public static final String URL_NEXT_INDEX = "next_index";
    public static final String URL_PREV_INDEX = "prev_index";
    public static final String URL_VALIDATE_FORM = "validate_form";
    public static final String URL_GET_INSTANCE = "get-instance";
    public static final String URL_CHANGE_LANGUAGE = "change_locale";
    public static final String URL_BREAK_LOCKS = "break_locks";
    public static final String URL_CHECK_LOCKS = "check_locks";

    // Alternative namings used by SMS
    public static final String URL_NEXT = "next";

    // Debugger URLS
    public static final String URL_DEBUGGER_FORMATTED_QUESTIONS = "formatted_questions";
    public static final String URL_DEBUGGER_MENU_CONTENT = "menu_debugger_content";

    // Multipart part names
    public static final String PART_FILE = "file";
    public static final String PART_ANSWER = "answer";

    // Change this version when a backwards incompatible change is made to the
    // mobile sqlite dbs.
    public static final String SQLITE_DB_VERSION = "V7";
    public static final String CASE_SEARCH_DB_VERSION = "V1";

    //Menus
    public static final String MENU_MODULE = "modules";
    public static final String MENU_ENTITY = "entity";
    public static final String CASE_LIST_ACTION = "action";
    //Status
    public static final String ANSWER_RESPONSE_STATUS_POSITIVE = "accepted";
    public static final String ANSWER_RESPONSE_STATUS_NEGATIVE = "validation-error";

    public static final String SUBMIT_RESPONSE_STATUS_POSITIVE = "success";
    public static final String SUBMIT_RESPONSE_TOO_MANY_REQUESTS = "too-many-requests";
    public static final String SUBMIT_RESPONSE_CASE_CYCLE_ERROR = "case-cycle-error";
    public static final String SUBMIT_RESPONSE_ERROR = "error";

    //Debug output request types
    public static final String BASIC_NO_TRACE = "basic";
    public static final String TRACE_REDUCE = "reduce";
    public static final String TRACE_FULL = "deep";


    // Error return types
    public static final String ERROR_TYPE_TEXT = "text";
    public static final String ERROR_TYPE_HTML = "html";

    // Error status types
    public static final String ERROR_STATUS = "error";
    public static final String RETRY_STATUS = "retry";

    //JSON
    public static final String QUESTION_TREE_KEY = "tree";
    public static final String NAV_MODE_PROMPT = "prompt";

    // CCZ Parameters
    public static final String CCZ_LATEST_SAVED = "save";

    // Postgres tables
    public static final String POSTGRES_TOKEN_TABLE_NAME = "django_session";
    // Token table generated from django rest framework
    public static final String POSTGRES_AUTH_TOKEN_TABLE_NAME = "authtoken_token";
    public static final String POSTGRES_USER_TABLE_NAME = "auth_user";
    public static final String POSTGRES_MENU_SESSION_TABLE_NAME = "menu_sessions";
    public static final String POSTGRES_VIRTUAL_DATA_INSTANCE_TABLE_NAME = "virtual_data_instance";

    public static final String POSTGRES_MEDIA_META_DATA_TABLE_NAME = "media_meta_data";

    public static final String SESSION_DETAILS_VIEW = "/hq/admin/session_details/";

    // Couch databases
    public static final String COUCH_USERS_DB = "__users";

    public static final String POSTGRES_DJANGO_SESSION_ID = "sessionid";
    public static final String COMMCARE_USER_SUFFIX = "commcarehq.org";

    public static final int USER_LOCK_TIMEOUT = 21;
    // 15 minutes in milliseconds
    public static final int LOCK_DURATION = 60 * 15 * 1000;
    public static final int CONNECT_TIMEOUT = 60 * 1000;
    public static final int READ_TIMEOUT = LOCK_DURATION;

    //Misc
    public static String HMAC_HEADER = "X-MAC-DIGEST";
    public static String HMAC_REQUEST_ATTRIBUTE = "org.commcare.formplayer.hmacRequest";

    // Datadog metrics
    public static final String DATADOG_TIMINGS = "timings";
    public static final String DATADOG_GRANULAR_TIMINGS = "granular.timings";
    public static final String DATADOG_RESTORE_COUNT = "restore.count";

    // Datadog/Sentry tags
    public static final String APP_NAME_TAG = "app_name";
    public static final String DOMAIN_TAG = "domain";
    public static final String FORM_NAME_TAG = "form_name";
    public static final String MODULE_TAG = "module";
    public static final String MODULE_NAME_TAG = "module_name";
    public static final String REQUEST_TAG = "request";
    public static final String CATEGORY_TAG = "category";
    public static final String DURATION_TAG = "duration";
    public static final String REQUEST_INITIATED_BY_TAG = "request_initiated_by";


    // Datadog tags assigned true/false values
    public static final String REQUEST_INCLUDES_AUTOSELECT_TAG = "request_includes_autoselect";
    public static final String REQUEST_INCLUDES_COMPLETE_RESTORE = "request_includes_complete_restore";
    public static final String REQUEST_INCLUDES_APP_INSTALL = "request_includes_app_install";
    public static final String TAG_VALUE_TRUE = "true";

    //.Sentry tags
    public static final String URI = "uri";
    public static final String AS_USER = "as_user";
    public static final String USER_SYNC_TOKEN = "sync_token";
    public static final String USER_SANDBOX_PATH = "sandbox_path";
    public static final String APP_URL_EXTRA = "app_url";
    public static final String APP_DOWNLOAD_URL_EXTRA = "app_download";


    public static final class TimingCategories {
        public static final String WAIT_ON_LOCK = "wait_on_lock";
        public static final String SUBMIT_FORM_TO_HQ = "submit_form_to_hq";
        public static final String APP_INSTALL = "app_install";
        public static final String PURGE_CASES = "purge_cases";
        public static final String PARSE_RESTORE = "parse_restore";
        public static final String DOWNLOAD_RESTORE = "download_restore";
        public static final String COMPLETE_RESTORE = "complete_restore";
        public static final String VALIDATE_SUBMISSION = "validate_submission";
        public static final String VALIDATE_ANSWERS = "validate_answers";
        public static final String END_OF_FORM_NAV = "end_of_form_navigation";
        public static final String FORM_ENTRY = "form_entry";
        public static final String UPDATE_VOLATILITY = "update_volatility";
        public static final String CREATE_FORM_CONTEXT = "create_form_context";
        public static final String PROCESS_AND_SUBMIT_FORM = "process_and_submit_form";
        public static final String PROCESS_FORM_XML = "process_form_xml";

        public static final String GET_SESSION = "get_session";
        public static final String INITIALIZE_SESSION = "initialize_session";
        public static final String PROCESS_ANSWER = "process_answer";
        public static final String PROCESS_MEDIA = "process_media";
        public static final String UPDATE_SESSION = "update_session";
        public static final String COMPILE_RESPONSE = "compile_response";
        public static final String BUILD_RESTORE_URL = "build_restore_url";
    }

    // Requests
    public static final String SUBMIT_ALL_REQUEST = "submit-all";
    public static final String ANSWER_REQUEST = "answer";
    public static final String NAV_MENU_REQUEST = "navigate_menu";

    // Errors
    public static final String DATADOG_ERRORS_APP_CONFIG = "errors.app_config";
    public static final String DATADOG_ERRORS_EXTERNAL_REQUEST = "errors.external_request";
    public static final String DATADOG_ERRORS_CRASH = "errors.crash";
    public static final String DATADOG_ERRORS_LOCK = "errors.lock";
    public static final String DATADOG_ERRORS_NOTIFICATIONS = "errors.notifications";

    // Cache Names
    public static final String VIRTUAL_DATA_INSTANCES_CACHE = "virtual_data_instances";
    public static final String MEDIA_METADATA_CACHE = "media_metadata";

    // End Datadog metrics

    public static final String SCHEDULED_TASKS_PURGE = "scheduled_tasks.purge";

    // Feature Flags/Toggles
    // These correspond to the names of StaticToggle objects in commcare-hq repo's corehq/toggles.py

    public static final String TOGGLE_DETAILED_TAGGING = "DETAILED_TAGGING";
    public static final String TOGGLE_SESSION_ENDPOINTS = "SESSION_ENDPOINTS";
    public static final String TOGGLE_SPLIT_SCREEN_CASE_SEARCH = "SPLIT_SCREEN_CASE_SEARCH";
    public static final String TOGGLE_INCLUDE_STATE_HASH = "FORMPLAYER_INCLUDE_STATE_HASH";

    public static final String AUTHORITY_COMMCARE = "COMMCARE";
}
