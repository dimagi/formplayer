package beans.menus;

import io.swagger.annotations.ApiModel;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.suite.model.Action;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.EntityDatum;
import org.commcare.util.cli.EntityDetailSubscreen;
import org.commcare.util.cli.EntityListSubscreen;
import org.commcare.util.cli.EntityScreen;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xpath.XPathException;
import util.SessionUtils;

import java.util.Arrays;
import java.util.Vector;

/**
 * Created by willpride on 4/13/16.
 */
@ApiModel("Entity List Response")
public class EntityListResponse extends MenuBean {
    private Entity[] entities;
    private DisplayElement action;
    private Style[] styles;
    private String[] headers;
    private int[] widthHints;

    private int CASE_LENGTH_LIMIT = 50;

    public EntityListResponse(){}

    public EntityListResponse(EntityScreen nextScreen) {
        SessionWrapper session = nextScreen.getSession();
        Detail shortDetail = nextScreen.getShortDetail();
        EvaluationContext ec = session.getEvaluationContext();
        Vector<TreeReference> references = ec.expandReference(((EntityDatum)session.getNeededDatum()).getNodeset());
        processTitle(session);
        processEntities(nextScreen, references, ec);
        processStyles(shortDetail);
        processActions(shortDetail, nextScreen.getSession());
        processHeader(shortDetail, ec);
    }

    private void processHeader(Detail shortDetail, EvaluationContext ec) {
        Pair<String[], int[]> mPair = EntityListSubscreen.getHeaders(shortDetail, ec);
        headers = mPair.first;
        widthHints = mPair.second;
    }

    private void processTitle(SessionWrapper session) {
        setTitle(SessionUtils.getBestTitle(session));
    }

    private void processEntities(EntityScreen screen, Vector<TreeReference> references, EvaluationContext ec) {
        Entity[] entities = generateEntities(screen, references, ec);

    }

    private Entity[] generateEntities(EntityScreen screen, Vector<TreeReference> references, EvaluationContext ec){
        Entity[] entities = new Entity[references.size()];
        int i = 0;
        for (TreeReference entity : references) {
            Entity newEntity = processEntity(entity, screen, ec);
            newEntity.setDetail(new EntityDetailResponse(processDetails(screen, ec, entity)));
            entities[i] = newEntity;
            i++;
        }
        return entities;
    }

    private Entity processEntity(TreeReference entity, EntityScreen screen, EvaluationContext ec) {
        Detail detail = screen.getShortDetail();
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

    private EntityDetailSubscreen processDetails(EntityScreen screen, EvaluationContext ec, TreeReference ref){
        EvaluationContext subContext = new EvaluationContext(ec, ref);
        //TODO WSP: don't hardcode first screen
        return new EntityDetailSubscreen(0, screen.getLongDetailList()[0],
                subContext, screen.getDetailListTitles(subContext));
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

    private void processActions(Detail detail, SessionWrapper session){
        Vector<Action> actions = session.getDetail(((EntityDatum)session.getNeededDatum()).getShortDetail()).getCustomActions();
        // Assume we only have one TODO WSP: is that correct?
        if(actions != null && !actions.isEmpty()) {
            Action action = actions.firstElement();
            setAction(new DisplayElement(action, session.getEvaluationContext()));
        }
    }

    public Entity[] getEntities() {
        return entities;
    }

    public void setEntities(Entity[] entities) {
        this.entities = entities;
    }

    public Style[] getStyles() {
        return styles;
    }

    public void setStyles(Style[] styles) {
        this.styles = styles;
    }

    public DisplayElement getAction() {
        return action;
    }

    public void setAction(DisplayElement action) {
        this.action = action;
    }

    @Override
    public String toString(){
        return "EntityListResponse headers=" + Arrays.toString(headers) + " width hints=" + Arrays.toString(widthHints)
        + " Entities=" + Arrays.toString(entities) + ", styles=" + Arrays.toString(styles) +
                ", action=" + action + " parent=" + super.toString();
    }

    public String[] getHeaders() {
        return headers;
    }

    public void setHeaders(String[] headers) {
        this.headers = headers;
    }

    public int[] getWidthHints() {
        return widthHints;
    }

    public void setWidthHints(int[] widthHints) {
        this.widthHints = widthHints;
    }
}
