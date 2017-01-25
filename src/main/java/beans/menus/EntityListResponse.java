package beans.menus;

import io.swagger.annotations.ApiModel;
import org.commcare.cases.entity.*;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
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
    private DisplayElement action;
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

    public EntityListResponse(EntityScreen nextScreen, int offset, String searchText, String id) {
        SessionWrapper session = nextScreen.getSession();
        Detail shortDetail = nextScreen.getShortDetail();
        nextScreen.getLongDetailList();

        EvaluationContext ec = nextScreen.getEvalContext();

        Vector<TreeReference> references = ec.expandReference(((EntityDatum) session.getNeededDatum()).getNodeset());
        processTitle(session);
        processCaseTiles(shortDetail);
        processEntities(nextScreen, references, ec, offset, searchText);
        processStyles(shortDetail);
        processActions(nextScreen.getSession());
        processHeader(shortDetail, ec);
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

    private void processHeader(Detail shortDetail, EvaluationContext ec) {
        Pair<String[], int[]> pair = EntityListSubscreen.getHeaders(shortDetail, ec);
        headers = pair.first;
        widthHints = pair.second;
    }

    private void processEntities(EntityScreen screen, Vector<TreeReference> references,
                                 EvaluationContext ec,
                                 int offset,
                                 String searchText) {
        List<Entity<TreeReference>> entityList = buildEntityList(screen.getShortDetail(), ec, references, offset, searchText);
        entities = new EntityBean[entityList.size()];
        int i = 0;
        for (Entity<TreeReference> entity : entityList) {
            TreeReference treeReference = entity.getElement();
            EntityBean newEntityBean = processEntity(treeReference, screen, ec);
            entities[i] = newEntityBean;
            i++;
        }
    }

    private List<Entity<TreeReference>> filterEntities(String searchText, NodeEntityFactory nodeEntityFactory,
                                                       List<Entity<TreeReference>> full) {
        if (searchText != null && !"".equals(searchText)) {
            EntityStringFilterer filterer = new EntityStringFilterer(searchText.split(" "), false, false, nodeEntityFactory, full);
            full = filterer.buildMatchList();
        }
        return full;
    }

    private List<Entity<TreeReference>> paginateEntities(List<Entity<TreeReference>> matched,
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

    private List<Entity<TreeReference>> buildEntityList(Detail shortDetail,
                                                        EvaluationContext context,
                                                        Vector<TreeReference> references,
                                                        int offset,
                                                        String searchText) {
        NodeEntityFactory nodeEntityFactory = new NodeEntityFactory(shortDetail, context);
        nodeEntityFactory.prepareEntities();
        List<Entity<TreeReference>> full = new ArrayList<>();
        for (TreeReference reference: references) {
            full.add(nodeEntityFactory.getEntity(reference));
        }

        List<Entity<TreeReference>> matched = filterEntities(searchText, nodeEntityFactory, full);
        sort(matched, shortDetail);

        if (matched.size() > CASE_LENGTH_LIMIT && !(numEntitiesPerRow > 1)) {
            // we're doing pagination
            setCurrentPage(offset / CASE_LENGTH_LIMIT);
            setPageCount((int) Math.ceil((double) matched.size() / CASE_LENGTH_LIMIT));
            matched = paginateEntities(matched, offset);
        }
        return matched;
    }

    private void sort (List<Entity<TreeReference>> entityList, Detail shortDetail) {
        int[] order = shortDetail.getSortOrder();
        for (int i = 0; i < shortDetail.getFields().length; ++i) {
            String header = shortDetail.getFields()[i].getHeader().evaluate();
            if (order.length == 0 && !"".equals(header)) {
                order = new int[]{i};
            }
        }
        java.util.Collections.sort(entityList, new EntitySorter(shortDetail.getFields(), false, order, new LogNotifier()));
    }

    class LogNotifier implements EntitySortNotificationInterface {
        @Override
        public void notifyBadfilter(String[] args) {

        }
    }

    private EntityBean processEntity(TreeReference entity, EntityScreen screen, EvaluationContext ec) {
        Detail detail = screen.getShortDetail();
        EvaluationContext context = new EvaluationContext(ec, entity);

        detail.populateEvaluationContextVariables(context);

        DetailField[] fields = detail.getFields();
        Object[] data = new Object[fields.length];

        String id = screen.getReturnValueFromSelection(entity, (EntityDatum)screen.getSession().getNeededDatum(), ec);
        EntityBean ret = new EntityBean(id);
        int i = 0;
        for (DetailField field : fields) {
            Object o;
            o = field.getTemplate().evaluate(context);
            data[i] = o;
            i++;
        }
        ret.setData(data);
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
