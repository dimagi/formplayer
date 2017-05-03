package beans.menus;

import org.commcare.modern.util.Pair;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.EntityDatum;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * Represents one detail tab with nodeset in a case details page.
 */
public class EntityDetailNodesetResponse {
    private EntityBean[] entities;
    private Style[] styles;
    private String[] headers;
    private String title;

    private boolean useNodeset = true;

    public EntityDetailNodesetResponse(Detail detail,
                                       Vector<TreeReference> references,
                                       EvaluationContext ec,
                                       EntityDatum neededDatum) {
        List<EntityBean> entityList = EntityListResponse.processEntitiesForCaseList(detail, references, ec, null, neededDatum);
        entities = new EntityBean[entityList.size()];
        entityList.toArray(entities);
        this.title = "Detail";
        this.styles = processStyles(detail);
        Pair<String[], int[]> pair = EntityListResponse.processHeader(detail, ec);
        this.headers = pair.first;
    }

    private static Style[] processStyles(Detail detail) {
        DetailField[] fields = detail.getFields();
        Style[] styles = new Style[fields.length];
        int i = 0;
        for (DetailField field : fields) {
            Style style = new Style(field);
            styles[i] = style;
            i++;
        }
        return styles;
    }

    public EntityBean[] getEntities() {
        return entities;
    }

    public void setEntities(EntityBean[] entities) {
        this.entities = entities;
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

    @Override
    public String toString(){
        return "EntityDetailNodesetResponse [entities=" + Arrays.toString(entities)
                + ", styles=" + Arrays.toString(styles)
                + ", headers=" + Arrays.toString(headers) + "]";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isUseNodeset() {
        return useNodeset;
    }

    public void setUseNodeset(boolean useNodeset) {
        this.useNodeset = useNodeset;
    }
}
