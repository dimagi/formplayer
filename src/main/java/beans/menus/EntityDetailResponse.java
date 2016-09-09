package beans.menus;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.util.cli.EntityDetailSubscreen;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Represents one detail tab in a case details page.
 */
public class EntityDetailResponse {
    private Object[] details;
    private Style[] styles;
    private String[] headers;
    private String title;

    public EntityDetailResponse(){}

    public EntityDetailResponse(EntityDetailSubscreen entityScreen){
        //TODO Get correct details title?
        this.setTitle("Details");
        this.details = entityScreen.getData();
        this.headers = entityScreen.getHeaders();
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
        ArrayList<Style> styleArrayList = new ArrayList<>();
        for (DetailField field : fields) {
            styleArrayList.add(new Style(field));
        }
        styles = (Style[]) styleArrayList.toArray();
    }

    @Override
    public String toString(){
        return "EntityDetailResponse [details=" + Arrays.toString(details)
                + ", styles=" + Arrays.toString(styles)
                + ", headers=" + Arrays.toString(headers) + "]";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
