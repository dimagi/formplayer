package beans.menus;

/**
 * Created by willpride on 4/13/16.
 */
public class MenuBean {

    private String title;

    public String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString(){
        return "MenuBean [title=" + title + "]";
    }
}
