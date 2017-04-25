package beans.menus;

import beans.NotificationMessageBean;
import org.commcare.modern.session.SessionWrapper;
import util.SessionUtils;

/**
 * Base class for responses being sent to the front end. Params are:
 * title - self explanatory
 * notification - A message String and error boolean to be displayed by frontend
 * clearSession - True if the front end should redirect to the app home
 *  NOTE: clearSession causes all other displayables to be disregarded
 */
public class BaseResponseBean {
    protected NotificationMessageBean notification;
    protected String title;
    protected boolean clearSession;
    private String appId;
    private String appVersion;
    private String[] selections;

    public BaseResponseBean() {}

    public BaseResponseBean(String title, String message, boolean isError, boolean clearSession){
        this.title = title;
        this.notification = new NotificationMessageBean(message, isError);
        this.clearSession = clearSession;
    }

    protected void processTitle(SessionWrapper session) {
        setTitle(SessionUtils.getBestTitle(session));
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public NotificationMessageBean getNotification() {
        return notification;
    }

    public void setNotification(NotificationMessageBean notification) {
        this.notification = notification;
    }

    public boolean isClearSession() {
        return clearSession;
    }

    public void setClearSession(boolean clearSession) {
        this.clearSession = clearSession;
    }

    @Override
    public String toString(){
        return "BaseResponseBean title=" + title + ", notificaiton=" + notification + ", " +
                "clearSession=" + clearSession;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String[] getSelections() {
        return selections;
    }

    public void setSelections(String[] selections) {
        this.selections = selections;
    }
}
