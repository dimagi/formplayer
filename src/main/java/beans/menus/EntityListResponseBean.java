package beans.menus;

import io.swagger.annotations.ApiModel;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.util.cli.EntityScreen;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xpath.XPathException;

import java.util.Vector;

/**
 * Created by willpride on 4/13/16.
 */
@ApiModel("Entity List Response")
public class EntityListResponseBean extends MenuSessionBean {
    private Entity[] entities;
    private boolean doubleManagementEnabled;
    private Style[] styles;

    public EntityListResponseBean(EntityScreen nextScreen) {
        this.setTitle(nextScreen.getScreenTitle());
        Detail detail = nextScreen.getShortDetail();
        EvaluationContext ec = nextScreen.getSession().getEvaluationContext();
        Vector<TreeReference> references = ec.expandReference(nextScreen.getSession().getNeededDatum().getNodeset());
        processEntities(detail, references, ec);
        processFields(detail);
    }

    private void processEntities(Detail detail, Vector<TreeReference> references, EvaluationContext ec) {
        entities = new Entity[references.size()];
        int i = 0;
        for (TreeReference entity : references) {
            Entity newEntity = processEntity(entity, detail, ec);
            entities[i] = newEntity;
            i++;
        }
    }

    private Entity processEntity(TreeReference entity, Detail detail, EvaluationContext ec) {
        Entity ret = new Entity();
        EvaluationContext context = new EvaluationContext(ec, entity);

        detail.populateEvaluationContextVariables(context);

        DetailField[] fields = detail.getFields();
        Object[] data = new Object[fields.length];

        int i = 0;
        for (DetailField field : fields) {
            Object o;
            try {
                o = field.getTemplate().evaluate(context);
            } catch(XPathException e) {
                o = "error (see output)";
                e.printStackTrace();
            }
            data[i] = o;
            i ++;
        }
        ret.setData(data);
        return ret;
    }

    private void processFields(Detail detail) {
        DetailField[] fields = detail.getFields();
        styles = new Style[fields.length];
        int i = 0;
        for (DetailField field : fields) {
            Style style = new Style();
            try {
                int fontSize = Integer.parseInt(field.getFontSize());
                style.setFontSize(fontSize);
            } catch(NumberFormatException e){
                // fine to ignore
            }
            String form = field.getTemplateForm();
            String widthHint = field.getTemplateWidthHint();
            style.setWidthHint(widthHint);
            style.setDisplayFormat(form);
            styles[i] = style;
            i++;
        }
    }

    public Entity[] getEntities() {
        return entities;
    }

    public void setEntities(Entity[] entities) {
        this.entities = entities;
    }

    public boolean isDoubleManagementEnabled() {
        return doubleManagementEnabled;
    }

    public void setDoubleManagementEnabled(boolean doubleManagementEnabled) {
        this.doubleManagementEnabled = doubleManagementEnabled;
    }

    public Style[] getStyles() {
        return styles;
    }

    public void setStyles(Style[] styles) {
        this.styles = styles;
    }
}
