package beans.menus;

import io.swagger.annotations.ApiModel;
import org.commcare.cases.entity.*;
import org.commcare.core.graph.model.GraphData;
import org.commcare.core.graph.util.GraphException;
import org.commcare.core.graph.util.GraphUtil;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.Action;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.EntityDatum;
import org.commcare.util.screen.EntityListSubscreen;
import org.commcare.util.screen.EntityScreen;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * Created by willpride on 4/13/16.
 */
@ApiModel("EntityBean List Response")
public class EntityListResponse extends MenuBean {
    private EntityBean[] entities;
    private DisplayElement[] actions;
    private Style[] styles;
    private String[] headers;
    private Tile[] tiles;
    private int[] widthHints;
    private int numEntitiesPerRow;
    private boolean useUniformUnits;

    private int pageCount;
    private int currentPage;
    private final String type = "entities";

    public static int CASE_LENGTH_LIMIT = 10;

    private boolean usesCaseTiles;
    private int maxWidth;
    private int maxHeight;

    public EntityListResponse() {}

    public EntityListResponse(EntityScreen nextScreen, String detailSelection, int offset, String searchText, String id) {
        SessionWrapper session = nextScreen.getSession();
        Detail detail = nextScreen.getShortDetail();
        EvaluationContext ec = nextScreen.getEvalContext();
        EntityDatum neededDatum = (EntityDatum) session.getNeededDatum();

        // When detailSelection is not null it means we're processing a case detail, not a case list.
        // We will shortcircuit the computation to just get the relevant detailSelection.
        if (detailSelection != null) {
            TreeReference reference = neededDatum.getEntityFromID(ec, detailSelection);
            entities = processEntitiesForCaseDetail(detail, reference, ec, neededDatum);
        } else {
            Vector<TreeReference> references = ec.expandReference(neededDatum.getNodeset());
            List<EntityBean> entityList = processEntitiesForCaseList(detail, references, ec, searchText, neededDatum);
            if (entityList.size() > CASE_LENGTH_LIMIT && !(detail.getNumEntitiesToDisplayPerRow() > 1)) {
                // we're doing pagination
                setCurrentPage(offset / CASE_LENGTH_LIMIT);
                setPageCount((int) Math.ceil((double) entityList.size() / CASE_LENGTH_LIMIT));
                entityList = paginateEntities(entityList, offset);
            }
            entities = new EntityBean[entityList.size()];
            entityList.toArray(entities);
        }


        processTitle(session);
        processCaseTiles(detail);
        this.styles = processStyles(detail);
        this.actions = processActions(nextScreen.getSession());
        Pair<String[], int[]> pair = processHeader(detail, ec);
        this.headers = pair.first;
        this.widthHints = pair.second;
        setMenuSessionId(id);
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
        useUniformUnits = shortDetail.useUniformUnitsInCaseTile();
        Pair<Integer, Integer> maxWidthHeight = shortDetail.getMaxWidthHeight();
        maxWidth = maxWidthHeight.first;
        maxHeight = maxWidthHeight.second;
    }

    public static Pair<String[], int[]> processHeader(Detail shortDetail, EvaluationContext ec) {
        return EntityListSubscreen.getHeaders(shortDetail, ec);
    }

    private static EntityBean[] processEntitiesForCaseDetail(Detail detail, TreeReference reference,
                                                             EvaluationContext ec, EntityDatum neededDatum) {
        return new EntityBean[]{processEntity(detail, reference, ec, neededDatum)};
    }

    public static List<EntityBean> processEntitiesForCaseList(Detail detail, Vector<TreeReference> references,
                                                               EvaluationContext ec,
                                                               String searchText, EntityDatum neededDatum) {
        List<Entity<TreeReference>> entityList = buildEntityList(detail, ec, references, searchText);
        List<EntityBean> entities = new ArrayList<>();
        for (Entity<TreeReference> entity : entityList) {
            TreeReference treeReference = entity.getElement();
            entities.add(processEntity(detail, treeReference, ec, neededDatum));
        }
        return entities;
    }

    private static List<Entity<TreeReference>> filterEntities(String searchText, NodeEntityFactory nodeEntityFactory,
                                                              List<Entity<TreeReference>> full) {
        if (searchText != null && !"".equals(searchText)) {
            EntityStringFilterer filterer = new EntityStringFilterer(searchText.split(" "), false, false, nodeEntityFactory, full);
            full = filterer.buildMatchList();
        }
        return full;
    }

    private List<EntityBean> paginateEntities(List<EntityBean> matched,
                                                         int offset) {
        if(offset > matched.size()){
            throw new RuntimeException("Pagination offset " + offset +
                    " exceeded case list length: " + matched.size());
        }

        int end = offset + CASE_LENGTH_LIMIT;
        int length = CASE_LENGTH_LIMIT;
        if (end > matched.size()) {
            end = matched.size();
            length = end - offset;
        }
        setPageCount((int) Math.ceil((double) matched.size() / CASE_LENGTH_LIMIT));
        matched = matched.subList(offset, offset + length);
        return matched;
    }

    private static List<Entity<TreeReference>> buildEntityList(Detail shortDetail,
                                                               EvaluationContext context,
                                                               Vector<TreeReference> references,
                                                               String searchText) {
        NodeEntityFactory nodeEntityFactory = new NodeEntityFactory(shortDetail, context);
        nodeEntityFactory.prepareEntities();
        List<Entity<TreeReference>> full = new ArrayList<>();
        for (TreeReference reference: references) {
            full.add(nodeEntityFactory.getEntity(reference));
        }

        List<Entity<TreeReference>> matched = filterEntities(searchText, nodeEntityFactory, full);
        sort(matched, shortDetail);
        return matched;
    }

    private static void sort(List<Entity<TreeReference>> entityList, Detail shortDetail) {
        int[] order = shortDetail.getOrderedFieldIndicesForSorting();
        for (int i = 0; i < shortDetail.getFields().length; ++i) {
            String header = shortDetail.getFields()[i].getHeader().evaluate();
            if (order.length == 0 && !"".equals(header)) {
                order = new int[]{i};
            }
        }
        java.util.Collections.sort(entityList, new EntitySorter(shortDetail.getFields(), false, order, new LogNotifier()));
    }

    static class LogNotifier implements EntitySortNotificationInterface {
        @Override
        public void notifyBadFilter(String[] args) {

        }
    }

    private static EntityBean processEntity(Detail detail, TreeReference treeReference,
                                            EvaluationContext ec, EntityDatum neededDatum) {
        EvaluationContext context = new EvaluationContext(ec, treeReference);
        detail.populateEvaluationContextVariables(context);
        DetailField[] fields = detail.getFields();
        Object[] data = new Object[fields.length];
        String id = EntityScreen.getReturnValueFromSelection(treeReference, neededDatum, ec);
        EntityBean ret = new EntityBean(id);
        int i = 0;
        for (DetailField field : fields) {
            Object o;
            o = field.getTemplate().evaluate(context);
            if(o instanceof GraphData) {
                try {
                    data[i] = GraphUtil.getHTML((GraphData) o, "").replace("\"", "'");
                } catch (GraphException e) {
                    data[i] = "<html><body>Error loading graph " + e + "</body></html>";
                }
            } else {
                data[i] = o;
            }
            i++;
        }
        ret.setData(data);
        return ret;
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

    private static DisplayElement[] processActions(SessionWrapper session) {
        EntityDatum datum = (EntityDatum) session.getNeededDatum();
        if (session.getFrame().getSteps().lastElement().getElementType().equals(SessionFrame.STATE_QUERY_REQUEST)) {
            return null;
        }
        Vector<Action> actions = session.getDetail((datum).getShortDetail()).getCustomActions(session.getEvaluationContext());
        ArrayList<DisplayElement> displayActions = new ArrayList<>();
        for (Action action: actions) {
            displayActions.add(new DisplayElement(action, session.getEvaluationContext()));
        }
        DisplayElement[] ret = new DisplayElement[actions.size()];
        displayActions.toArray(ret);
        return ret;
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

    public DisplayElement[] getActions() {
        return actions;
    }

    private void setActions(DisplayElement[] actions) {
        this.actions = actions;
    }

    @Override
    public String toString() {
        return "EntityListResponse [Entities=" + Arrays.toString(entities) + ", styles=" + Arrays.toString(styles) +
                ", action=" + Arrays.toString(actions) + " parent=" + super.toString() + ", headers=" + Arrays.toString(headers) +
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

    public int getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public boolean isUseUniformUnits() {
        return useUniformUnits;
    }

    public void setUseUniformUnits(boolean useUniformUnits) {
        this.useUniformUnits = useUniformUnits;
    }
}
