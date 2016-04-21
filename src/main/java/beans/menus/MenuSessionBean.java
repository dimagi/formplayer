package beans.menus;

import beans.SessionBean;

/**
 * Created by willpride on 4/13/16.
 */
public class MenuSessionBean extends SessionBean {

    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString(){
        return "MenuSessionBean [title=" + title + ", SessionBean= " + super.toString() + "]";
    }
}
