package org.commcare.formplayer.beans.menus;

import datadog.trace.api.Trace;

import org.commcare.cases.entity.Entity;
import org.commcare.cases.entity.EntitySortNotificationInterface;
import org.commcare.cases.entity.EntitySorter;
import org.commcare.cases.entity.NodeEntityFactory;
import org.commcare.core.graph.model.GraphData;
import org.commcare.core.graph.util.GraphException;
import org.commcare.formplayer.exceptions.ApplicationConfigException;
import org.commcare.formplayer.util.EntityStringFilterer;
import org.commcare.formplayer.util.FormplayerGraphUtil;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.suite.model.Action;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.Style;
import org.commcare.util.screen.EntityListSubscreen;
import org.commcare.util.screen.EntityScreen;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.function.Predicate;

/**
 * Created by willpride on 4/13/16.
 */
public class EntityListResponse extends MenuBean {
    private EntityBean[] entities;
    private DisplayElement[] actions;
    private String redoLast;
    private Style[] styles;
    private String[] headers;
    private Tile[] tiles;
    private int[] widthHints;
    private int numEntitiesPerRow;
    private boolean useUniformUnits;
    private int[] sortIndices;

    private int pageCount;
    private int currentPage;
    private final String type = "entities";

    private static int DEFAULT_CASES_PER_PAGE = 10;
    private static int MAX_CASES_PER_PAGE = 100;

    private boolean usesCaseTiles;
    private int maxWidth;
    private int maxHeight;

    public EntityListResponse() {
    }

    public EntityListResponse(EntityScreen nextScreen,
                              String detailSelection,
                              int offset,
                              String searchText,
                              int sortIndex,
                              boolean isFuzzySearchEnabled,
                              int casesPerPage) {
        SessionWrapper session = nextScreen.getSession();
        Detail detail = nextScreen.getShortDetail();
        EntityDatum neededDatum = (EntityDatum)session.getNeededDatum();
        EvaluationContext ec = nextScreen.getEvalContext();

        this.actions = processActions(nextScreen.getSession());
        this.redoLast = processRedoLast(nextScreen.getSession());

        // When detailSelection is not null it means we're processing a case detail, not a case list.
        // We will shortcircuit the computation to just get the relevant detailSelection.
        if (detailSelection != null) {
            TreeReference reference = neededDatum.getEntityFromID(ec, detailSelection);
            if (reference == null) {
                throw new ApplicationConfigException(String.format("Could not create detail %s for case with ID %s " +
                        " either because the case filter matched zero or multiple cases.", detailSelection, detail));
            }
            Detail[] longDetails = nextScreen.getLongDetailList(reference);
            if (longDetails != null) {
                detail = longDetails[0];
            }
            entities = processEntitiesForCaseDetail(detail, reference, ec, neededDatum);
        } else {
            Vector<TreeReference> references = nextScreen.getReferences();
            List<EntityBean> entityList = processEntitiesForCaseList(detail, references, ec, searchText, neededDatum, sortIndex, isFuzzySearchEnabled);

            if (casesPerPage == 0) {
                casesPerPage = DEFAULT_CASES_PER_PAGE;
            }
            casesPerPage = Math.min(casesPerPage, MAX_CASES_PER_PAGE);

            if (entityList.size() > casesPerPage && !(detail.getNumEntitiesToDisplayPerRow() > 1)) {
                // we're doing pagination
                setCurrentPage(offset / casesPerPage);
                setPageCount((int)Math.ceil((double)entityList.size() / casesPerPage));
                entityList = paginateEntities(entityList, casesPerPage, offset);
            }
            entities = new EntityBean[entityList.size()];
            entityList.toArray(entities);
        }

        processTitle(session);
        processCaseTiles(detail);
        this.styles = processStyles(detail);
        Pair<String[], int[]> pair = processHeader(detail, ec, sortIndex);
        this.headers = pair.first;
        this.widthHints = pair.second;
        this.sortIndices = detail.getOrderedFieldIndicesForSorting();
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

    public static Pair<String[], int[]> processHeader(Detail shortDetail, EvaluationContext ec, int sortIndex) {
        return EntityListSubscreen.getHeaders(shortDetail, ec, sortIndex);
    }

    private static EntityBean[] processEntitiesForCaseDetail(Detail detail, TreeReference reference,
                                                             EvaluationContext ec, EntityDatum neededDatum) {
        return new EntityBean[]{processEntity(detail, reference, ec, neededDatum)};
    }

    @Trace
    public static List<EntityBean> processEntitiesForCaseList(Detail detail, Vector<TreeReference> references,
                                                              EvaluationContext ec,
                                                              String searchText,
                                                              EntityDatum neededDatum,
                                                              int sortIndex,
                                                              boolean isFuzzySearchEnabled) {
        List<Entity<TreeReference>> entityList = buildEntityList(detail, ec, references, searchText, sortIndex, isFuzzySearchEnabled);
        List<EntityBean> entities = new ArrayList<>();
        for (Entity<TreeReference> entity : entityList) {
            TreeReference treeReference = entity.getElement();
            entities.add(processEntity(detail, treeReference, ec, neededDatum));
        }
        return entities;
    }

    @Trace
    private static List<Entity<TreeReference>> filterEntities(String searchText, NodeEntityFactory nodeEntityFactory,
                                                              List<Entity<TreeReference>> full, boolean isFuzzySearchEnabled) {
        if (searchText != null && !"".equals(searchText)) {
            EntityStringFilterer filterer = new EntityStringFilterer(searchText.split(" "), nodeEntityFactory, full, isFuzzySearchEnabled);
            full = filterer.buildMatchList();
        }
        return full;
    }

    @Trace
    private List<EntityBean> paginateEntities(List<EntityBean> matched, int casesPerPage,
                                              int offset) {
        if (offset > matched.size()) {
            throw new RuntimeException("Pagination offset " + offset +
                    " exceeded case list length: " + matched.size());
        }

        int end = offset + casesPerPage;
        int length = casesPerPage;
        if (end > matched.size()) {
            end = matched.size();
            length = end - offset;
        }
        setPageCount((int)Math.ceil((double)matched.size() / casesPerPage));
        matched = matched.subList(offset, offset + length);
        return matched;
    }

    @Trace
    private static List<Entity<TreeReference>> buildEntityList(Detail shortDetail,
                                                               EvaluationContext context,
                                                               Vector<TreeReference> references,
                                                               String searchText,
                                                               int sortIndex,
                                                               boolean isFuzzySearchEnabled) {
        NodeEntityFactory nodeEntityFactory = new NodeEntityFactory(shortDetail, context);
        List<Entity<TreeReference>> full = new ArrayList<>();
        for (TreeReference reference : references) {
            full.add(nodeEntityFactory.getEntity(reference));
        }
        nodeEntityFactory.prepareEntities(full);
        List<Entity<TreeReference>> matched = filterEntities(searchText, nodeEntityFactory, full, isFuzzySearchEnabled);
        sort(matched, shortDetail, sortIndex);
        return matched;
    }

    @Trace
    private static void sort(List<Entity<TreeReference>> entityList,
                             Detail shortDetail,
                             int sortIndex) {
        int[] order;
        boolean reverse = false;
        if (sortIndex != 0) {
            if (sortIndex < 0) {
                reverse = true;
                sortIndex = Math.abs(sortIndex);
            }
            // sort index is one indexed so adjust for that
            int sortFieldIndex = sortIndex - 1;
            order = new int[]{sortFieldIndex};
        } else {
            order = shortDetail.getOrderedFieldIndicesForSorting();
            for (int i = 0; i < shortDetail.getFields().length; ++i) {
                String header = shortDetail.getFields()[i].getHeader().evaluate();
                if (order.length == 0 && !"".equals(header)) {
                    order = new int[]{i};
                }
            }
        }
        java.util.Collections.sort(entityList, new EntitySorter(shortDetail.getFields(), reverse, order, new LogNotifier()));
    }

    public int[] getSortIndices() {
        return sortIndices;
    }

    public void setSortIndices(int[] sortIndices) {
        this.sortIndices = sortIndices;
    }

    static class LogNotifier implements EntitySortNotificationInterface {
        @Override
        public void notifyBadFilter(String[] args) {

        }
    }

    @Trace
    private static EntityBean processEntity(Detail detail, TreeReference treeReference,
                                            EvaluationContext ec, EntityDatum neededDatum) {
        EvaluationContext context = new EvaluationContext(ec, treeReference);
        detail.populateEvaluationContextVariables(context);
        DetailField[] fields = detail.getFields();
        Object[] data = new Object[fields.length];
        String id = neededDatum == null ? "" : EntityScreen.getReturnValueFromSelection(treeReference, neededDatum, ec);
        EntityBean ret = new EntityBean(id);
        int i = 0;
        for (DetailField field : fields) {
            Object o;
            o = field.getTemplate().evaluate(context);
            if (o instanceof GraphData) {
                try {
                    data[i] = FormplayerGraphUtil.getHTML((GraphData)o, "").replace("\"", "'");
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
        Vector<Action> actions = getActionDefinitions(session);
        ArrayList<DisplayElement> displayActions = new ArrayList<>();
        for (Action action : actions) {
            displayActions.add(new DisplayElement(action, session.getEvaluationContext()));
        }
        DisplayElement[] ret = new DisplayElement[actions.size()];
        displayActions.toArray(ret);
        return ret;
    }

    private static String processRedoLast(SessionWrapper session) {
        return formatActionsWithFilter(session, Action::isRedoLast);
    }

    private static String formatActionsWithFilter(SessionWrapper session, Predicate<Action> actionFilter) {
        Vector<Action> actions = getActionDefinitions(session);
        String ret = null;
        int index = 0;
        for (Action action : actions) {
            if (actionFilter.test(action)) {
                ret = "action " + index;
            }
            index = index + 1;
        }
        return ret;
    }


    private static Vector<Action> getActionDefinitions(SessionWrapper session) {
        EntityDatum datum = (EntityDatum)session.getNeededDatum();
        return session.getDetail((datum).getShortDetail()).getCustomActions(session.getEvaluationContext());
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

    public String getRedoLast() {
        return redoLast;
    }

    public void setRedoLast(String redoLast) {
        this.redoLast = redoLast;
    }

    @Override
    public String toString() {
        return "EntityListResponse [Title= " + getTitle() +
                ", styles=" + Arrays.toString(styles) +
                ", action=" + Arrays.toString(actions) +
                ", parent=" + super.toString() +
                ", headers=" + Arrays.toString(headers) +
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
