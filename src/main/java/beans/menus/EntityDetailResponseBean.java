package beans.menus;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.util.cli.EntityDetailSubscreen;
import org.commcare.util.cli.EntityScreen;
import util.SessionUtils;

/**
 * Created by willpride on 4/13/16.
 */
public class EntityDetailResponseBean extends MenuSessionBean{
    private Object[] details;
    private Style[] styles;
    private String[] headers;

    public EntityDetailResponseBean(){}

    public EntityDetailResponseBean(EntityScreen entityScreen){
        this.setTitle(SessionUtils.getBestTitle(entityScreen.getSession()));
        this.details = ((EntityDetailSubscreen)entityScreen.getCurrentScreen()).getData();
        this.headers = ((EntityDetailSubscreen)entityScreen.getCurrentScreen()).getHeaders();
    }

    public Object[] getDetails() {
        return details;
    }

    public void setDetails(Object[] data) {
        this.details = data;
    }

    public Style[] getStyles() {
        return styles;
    }

    public void setStyles(Style[] styles) {
        this.styles = styles;
    }

    public String[] getHeaders() {
        return headers;
    }

    public void setHeaders(String[] headers) {
        this.headers = headers;
    }

    private void processStyles(Detail detail) {
        DetailField[] fields = detail.getFields();
        styles = new Style[fields.length];
        int i = 0;
        for (DetailField field : fields) {
            Style style = new Style(field);
            styles[i] = style;
            i++;
        }
    }
}
