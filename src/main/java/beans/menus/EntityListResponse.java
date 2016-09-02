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

import java.util.ArrayList;
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
    private Tile[] tiles;
    private int[] widthHints;
    private int numEntitiesPerRow;

    private int pageCount;
    private int currentPage;
    private final String type = "entities";

    public static final int CASE_LENGTH_LIMIT = 100;

    private boolean usesCaseTiles;

    public EntityListResponse() {
    }

    public EntityListResponse(EntityScreen nextScreen, int offset, String searchText, String id) {
        SessionWrapper session = nextScreen.getSession();
        Detail shortDetail = nextScreen.getShortDetail();
        nextScreen.getLongDetailList();

        EvaluationContext ec = session.getEvaluationContext();

        Vector<TreeReference> references = ec.expandReference(((EntityDatum) session.getNeededDatum()).getNodeset());
        processTitle(session);
        processEntities(nextScreen, references, ec, offset, searchText);
        processStyles(shortDetail);
        processActions(nextScreen.getSession());
        processHeader(shortDetail, ec);
        setMenuSessionId(id);
        processCaseTiles(shortDetail);
    }

    private void processCaseTiles(Detail shortDetail) {
        DetailField[] fields = shortDetail.getFields();
        if (!shortDetail.usesEntityTileView()) {
            return;
        }
        tiles = new Tile[fields.length];
        setUsesCaseTiles(true);
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].isCaseTileField()) {
                tiles[i] = new Tile(fields[i]);
            } else {
                tiles[i] = null;
            }
        }
        numEntitiesPerRow = shortDetail.getNumEntitiesToDisplayPerRow();
    }

    private void processHeader(Detail shortDetail, EvaluationContext ec) {
        Pair<String[], int[]> pair = EntityListSubscreen.getHeaders(shortDetail, ec);
        headers = pair.first;
        widthHints = pair.second;
    }

    private void processEntities(EntityScreen screen, Vector<TreeReference> references,
                                 EvaluationContext ec,
                                 int offset,
                                 String searchText) {
        Entity[] allEntities = generateEntities(screen, references, ec);
        if (searchText != null && !searchText.trim().equals("")) {
            allEntities = filterEntities(allEntities, searchText);
        }
        if (allEntities.length > CASE_LENGTH_LIMIT) {
            // we're doing pagination

            if(offset > allEntities.length){
                throw new RuntimeException("Pagination offset " + offset +
                        " exceeded case list length: " + allEntities.length);
            }

            int end = offset + CASE_LENGTH_LIMIT;
            int length = CASE_LENGTH_LIMIT;
            if (end > allEntities.length) {
                end = allEntities.length;
                length = end - offset;
            }
            entities = new Entity[length];
            System.arraycopy(allEntities, offset, entities, offset - offset, end - offset);

            setPageCount((int) Math.ceil((double) allEntities.length / CASE_LENGTH_LIMIT));
            setCurrentPage(offset / CASE_LENGTH_LIMIT);
        } else {
            entities = allEntities.clone();
        }
    }

    /**
     * @param allEntities the set of Entity objects to filter
     * @param searchText  the raw (space separated) searchText entry to split and filter with
     * @return all Entitys containing one of the searchText strings
     */
    private Entity[] filterEntities(Entity[] allEntities, String searchText) {

        ArrayList<Entity> compiler = new ArrayList<Entity>();
        for (Entity entity : allEntities) {
            if (matchEntity(entity, searchText)) {
                compiler.add(entity);
            }
        }
        Entity[] ret = new Entity[compiler.size()];
        ret = compiler.toArray(ret);
        return ret;
    }

    /**
     * @param entity     The Entity being matched against
     * @param searchText The String being searched for
     * @return whether the Entity's data[] contains this string, ignoring case
     */
    private boolean matchEntity(Entity entity, String searchText) {
        String[] searchStrings = searchText.split(" ");
        for (Object data : entity.getData()) {
            for (String searchString : searchStrings) {
                if (data != null && data.toString().toLowerCase().contains(searchString.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Entity[] generateEntities(EntityScreen screen, Vector<TreeReference> references, EvaluationContext ec) {
        Entity[] entities = new Entity[references.size()];
        int i = 0;
        for (TreeReference entity : references) {
            Entity newEntity = processEntity(entity, screen, ec);
            EntityDetailSubscreen[] subscreens = processDetails(screen, ec, entity);
            if (subscreens != null) {
                EntityDetailResponse[] responses = new EntityDetailResponse[subscreens.length];
                for (int j = 0; j < subscreens.length; j++) {
                    responses[j] = new EntityDetailResponse(subscreens[j]);
                    responses[j].setTitle(subscreens[j].getTitles()[j]);
                }
                newEntity.setDetails(responses);
            }
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
            } catch (XPathException e) {
                throw new RuntimeException(e);
            }
            data[i] = o;
            i++;
        }
        ret.setData(data);
        return ret;
    }

    private EntityDetailSubscreen[] processDetails(EntityScreen screen, EvaluationContext ec, TreeReference ref) {
        Detail[] detailList = screen.getLongDetailList();
        if (detailList == null || !(detailList.length > 0)) {
            // No details, just return null
            return null;
        }
        EvaluationContext subContext = new EvaluationContext(ec, ref);
        EntityDetailSubscreen[] ret = new EntityDetailSubscreen[detailList.length];
        for (int i = 0; i < detailList.length; i++) {
            ret[i] = new EntityDetailSubscreen(i, detailList[i], subContext, screen.getDetailListTitles(subContext));
        }
        return ret;
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

    private void processActions(SessionWrapper session) {
        Vector<Action> actions = session.getDetail(((EntityDatum) session.getNeededDatum()).getShortDetail()).getCustomActions(session.getEvaluationContext());
        // Assume we only have one TODO WSP: is that correct?
        if (actions != null && !actions.isEmpty()) {
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
    public String toString() {
        return "EntityListResponse [Entities=" + Arrays.toString(entities) + ", styles=" + Arrays.toString(styles) +
                ", action=" + action + " parent=" + super.toString() + ", headers=" + Arrays.toString(headers) +
                ", locales=" + Arrays.toString(getLocales()) + "]";
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

    public String getType() {
        return type;
    }

    public boolean getUsesCaseTiles() {
        return usesCaseTiles;
    }

    public void setUsesCaseTiles(boolean usesCaseTiles) {
        this.usesCaseTiles = usesCaseTiles;
    }

    public Tile[] getTiles() {
        return tiles;
    }

    public int getNumEntitiesPerRow() {
        return numEntitiesPerRow;
    }

    public void setNumEntitiesPerRow(int numEntitiesPerRow) {
        this.numEntitiesPerRow = numEntitiesPerRow;
    }
}
