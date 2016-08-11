package beans.menus;

import beans.NotificationMessageBean;

/**
 * Created by willpride on 8/11/16.
 */
public class BaseResponseBean {
    protected NotificationMessageBean notification;
    protected String title;

    public BaseResponseBean() {}

    public BaseResponseBean(String title, String message, boolean isError){
        this.title = title;
        this.notification = new NotificationMessageBean(message, isError);
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
}
