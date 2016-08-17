package beans;

import objects.SerializableFormSession;

/**
 * Individual display item in list of incomplete form sessions
 */
public class SessionListItem {

    private String title;
    private String dateOpened;

    public SessionListItem(SerializableFormSession session){
        this.title = session.getTitle();
        this.dateOpened = session.getDateOpened();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDateOpened() {
        return dateOpened;
    }

    public void setDateOpened(String dateOpened) {
        this.dateOpened = dateOpened;
    }
}
