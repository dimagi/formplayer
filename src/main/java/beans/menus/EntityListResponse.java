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

    private int pageCount;
    private int currentPage;

    public static final int CASE_LENGTH_LIMIT = 10;

    public EntityListResponse(){}

    public EntityListResponse(EntityScreen nextScreen){
        this(nextScreen, 0);
    }

    public EntityListResponse(EntityScreen nextScreen, int offset) {
        SessionWrapper session = nextScreen.getSession();
        Detail shortDetail = nextScreen.getShortDetail();
        EvaluationContext ec = session.getEvaluationContext();
        Vector<TreeReference> references = ec.expandReference(((EntityDatum)session.getNeededDatum()).getNodeset());
        processTitle(session);
        processEntities(nextScreen, references, ec, offset);
        processStyles(shortDetail);
        processActions(shortDetail, nextScreen.getSession());
        processHeader(shortDetail, ec);
    }

    private void processHeader(Detail shortDetail, EvaluationContext ec) {
        Pair<String[], int[]> pair = EntityListSubscreen.getHeaders(shortDetail, ec);
        headers = pair.first;
        widthHints = pair.second;
    }

    private void processTitle(SessionWrapper session) {
        setTitle(SessionUtils.getBestTitle(session));
    }

    private void processEntities(EntityScreen screen, Vector<TreeReference> references, EvaluationContext ec, int offset) {
        Entity[] allEntities = generateEntities(screen, references, ec);
        if(allEntities.length > CASE_LENGTH_LIMIT){
            // we're doing pagination
            int start = offset;
            int end = offset + CASE_LENGTH_LIMIT;
            int length = CASE_LENGTH_LIMIT;
            if(end > allEntities.length){
                end = allEntities.length;
                length = end - start;
            }
            entities = new Entity[length];
            for(int i = start; i< end; i++){
                entities[i-offset] = allEntities[i];
            }

            setPageCount((int)Math.ceil((double)allEntities.length/CASE_LENGTH_LIMIT));
            setCurrentPage(offset/CASE_LENGTH_LIMIT);
        } else{
            entities = allEntities.clone();
        }
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
                throw new RuntimeException(e);
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

    private void setAction(DisplayElement action) {
        this.action = action;
    }

    @Override
    public String toString(){
        return "EntityListResponse [Entities=" + Arrays.toString(entities) + ", styles=" + Arrays.toString(styles) +
                ", action=" + action + " parent=" + super.toString() + ", headers=" + Arrays.toString(headers) + "]";
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

    public int getPageCount() {
        return pageCount;
    }

    private void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    private void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
}
