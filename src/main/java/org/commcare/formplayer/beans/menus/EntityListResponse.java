package org.commcare.formplayer.beans.menus;

import org.commcare.cases.entity.Entity;
import org.commcare.core.graph.model.GraphData;
import org.commcare.core.graph.util.GraphException;
import org.commcare.formplayer.util.FormplayerGraphUtil;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.suite.model.Action;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.Endpoint;
import org.commcare.suite.model.EndpointAction;
import org.commcare.suite.model.EndpointArgument;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.Style;
import org.commcare.util.screen.EntityListSubscreen;
import org.commcare.util.screen.EntityScreen;
import org.commcare.util.screen.EntityScreenContext;
import org.commcare.util.screen.MultiSelectEntityScreen;
import org.commcare.util.screen.QueryScreen;
import org.commcare.util.screen.Subscreen;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.NoLocalizedTextException;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.function.Predicate;

import datadog.trace.api.Trace;

/**
 * Created by willpride on 4/13/16.
 */
public class EntityListResponse extends MenuBean {
    private EntityBean[] entities;
    private DisplayElement[] actions;
    private String redoLast;
    private Style[] styles;

    private EndpointActionResponse[] endpointActions;
    private String[] headers;
    private Tile[] tiles;
    private int[] widthHints;
    private int numEntitiesPerRow;
    private boolean useUniformUnits;
    private int[] sortIndices;
    private String noItemsText;

    private int pageCount;
    private int currentPage;
    private final String type = "entities";

    private static int MAX_CASES_PER_PAGE = 100;

    private boolean usesCaseTiles;
    private int maxWidth;
    private int maxHeight;

    private boolean isMultiSelect = false;
    private int maxSelectValue = -1;

    private boolean hasDetails = true;
    private QueryResponseBean queryResponse;

    private int groupHeaderRows = -1;

    public EntityListResponse() {
    }

    public EntityListResponse(EntityScreen nextScreen) {
        // This constructor can be called for both entity list and detail screens but
        // subscreen should be of type EntityListSubscreen in order to init this response class
        Subscreen subScreen = nextScreen.getCurrentScreen();
        if (subScreen instanceof EntityListSubscreen) {
            EntityListSubscreen entityListScreen = ((EntityListSubscreen)nextScreen.getCurrentScreen());
            Vector<Action> entityListActions = entityListScreen.getActions();
            this.actions = processActions(nextScreen.getSession(), entityListActions);
            this.redoLast = processRedoLast(entityListActions);

            List<Entity<TreeReference>> entityList = entityListScreen.getEntities();
            EntityScreenContext entityScreenContext = nextScreen.getEntityScreenContext();
            int casesPerPage = entityScreenContext.getCasesPerPage();
            casesPerPage = Math.min(casesPerPage, MAX_CASES_PER_PAGE);
            int offset = entityScreenContext.getOffSet();
            Detail detail = nextScreen.getShortDetail();
            List<Entity<TreeReference>> entitesForPage = paginateEntities(entityList, detail, casesPerPage,
                    offset);
            EvaluationContext ec = nextScreen.getEvalContext();
            SessionWrapper session = nextScreen.getSession();
            EntityDatum neededDatum = (EntityDatum)session.getNeededDatum();
            List<EntityBean> entityBeans = processEntitiesForCaseList(entitesForPage, ec, neededDatum);
            entities = new EntityBean[entityBeans.size()];
            entityBeans.toArray(entities);
            setNoItemsText(getNoItemsTextLocaleString(detail));
            hasDetails = nextScreen.getLongDetail() != null;

            processTitle(session);
            processCaseTiles(detail);
            this.styles = processStyles(detail);
            this.endpointActions = processEndpointActions(detail, nextScreen);
            int sortIndex = entityScreenContext.getSortIndex();
            Pair<String[], int[]> pair = processHeader(detail, ec, sortIndex);
            this.headers = pair.first;
            this.widthHints = pair.second;
            this.sortIndices = detail.getOrderedFieldIndicesForSorting();
            isMultiSelect = nextScreen instanceof MultiSelectEntityScreen;
            if (isMultiSelect) {
                maxSelectValue = ((MultiSelectEntityScreen)nextScreen).getMaxSelectValue();
            }
            if (detail.getGroup() != null) {
                groupHeaderRows = detail.getGroup().getHeaderRows();
            }

            QueryScreen queryScreen = nextScreen.getQueryScreen();
            if (queryScreen != null) {
                setQueryKey(queryScreen.getQueryKey());
                queryResponse = new QueryResponseBean(queryScreen);
            }
        }
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

    public static Pair<String[], int[]> processHeader(Detail shortDetail, EvaluationContext ec,
            int sortIndex) {
        return EntityListSubscreen.getHeaders(shortDetail, ec, sortIndex);
    }

    private static EntityBean[] processEntitiesForCaseDetail(Detail detail, TreeReference reference,
            EvaluationContext ec, EntityDatum neededDatum) {
        return new EntityBean[]{evalEntity(detail, reference, ec, neededDatum)};
    }

    @Trace
    public static List<EntityBean> processEntitiesForCaseList(List<Entity<TreeReference>> entityList,
            EvaluationContext ec,
            EntityDatum neededDatum) {
        List<EntityBean> entities = new ArrayList<>();
        for (Entity<TreeReference> entity : entityList) {
            entities.add(toEntityBean(entity, ec, neededDatum));
        }
        return entities;
    }

    private List<Entity<TreeReference>> paginateEntities(
            List<Entity<TreeReference>> entityList, Detail detail, int casesPerPage, int offset) {
        if (entityList.size() > casesPerPage && !(detail.getNumEntitiesToDisplayPerRow() > 1)) {
            // we're doing pagination
            return getEntitiesForCurrentPage(entityList, casesPerPage, offset, detail);
        }
        return entityList;
    }

    @Trace
    private List<Entity<TreeReference>> getEntitiesForCurrentPage(List<Entity<TreeReference>> matched,
            int casesPerPage,
            int offset, Detail detail) {
        setPageCount((int)Math.ceil((double)matched.size() / casesPerPage));
        if (offset > matched.size()) {
            // Set the offset to last page
            offset = casesPerPage * (getPageCount() - 1);
        }
        setCurrentPage(offset / casesPerPage);
        int end = offset + casesPerPage;
        List<Entity<TreeReference>> entitiesForPage = new ArrayList<>();
        if (detail.getGroup() != null) {
            // we want to paginate the groups insted of entities
            int groupCount = -1;
            for (int i = 0; i < matched.size(); i++) {
                Entity<TreeReference> currEntity = matched.get(i);
                if (i == 0 || !Objects.equals(currEntity.getGroupKey(), matched.get(i - 1).getGroupKey())) {
                    groupCount++;
                }
                if (groupCount >= offset && groupCount < end) {
                    entitiesForPage.add(currEntity);
                }
            }
            setPageCount((int)Math.ceil((double)(groupCount + 1) / casesPerPage));
        } else {
            int length = casesPerPage;
            if (end > matched.size()) {
                end = matched.size();
                length = end - offset;
            }
            entitiesForPage = matched.subList(offset, offset + length);
        }
        return entitiesForPage;
    }

    public int[] getSortIndices() {
        return sortIndices;
    }

    public void setSortIndices(int[] sortIndices) {
        this.sortIndices = sortIndices;
    }

    public boolean isHasDetails() {
        return hasDetails;
    }

    // Converts the Given Entity to EntityBean
    @Trace
    private static EntityBean toEntityBean(Entity<TreeReference> entity,
            EvaluationContext ec, EntityDatum neededDatum) {
        Object[] entityData = entity.getData();
        Object[] data = new Object[entityData.length];
        String id = getEntityId(entity.getElement(), neededDatum, ec);
        EntityBean entityBean = new EntityBean(id);
        for (int i = 0; i < entityData.length; i++) {
            data[i] = processData(entityData[i]);
        }
        entityBean.setData(data);
        entityBean.setGroupKey(entity.getGroupKey());
        return entityBean;
    }

    private static Object processData(Object data) {
        if (data instanceof GraphData) {
            try {
                return FormplayerGraphUtil.getHtml((GraphData)data, "").replace("\"", "'");
            } catch (GraphException e) {
                return "<html><body>Error loading graph " + e + "</body></html>";
            }
        } else {
            return data;
        }
    }

    // Evaluates detail fields for the given entity reference and returns it as EntityBean
    @Trace
    private static EntityBean evalEntity(Detail detail, TreeReference treeReference,
            EvaluationContext ec, EntityDatum neededDatum) {
        EvaluationContext context = new EvaluationContext(ec, treeReference);
        detail.populateEvaluationContextVariables(context);
        DetailField[] fields = detail.getFields();
        Object[] data = new Object[fields.length];
        String id = getEntityId(treeReference, neededDatum, ec);
        EntityBean ret = new EntityBean(id);
        int i = 0;
        for (DetailField field : fields) {
            Object o = field.getTemplate().evaluate(context);
            data[i] = processData(o);
            i++;
        }
        ret.setData(data);
        return ret;
    }

    private static String getEntityId(TreeReference treeReference, EntityDatum neededDatum, EvaluationContext ec) {
        return neededDatum == null ? "" : EntityScreen.getReturnValueFromSelection(
                treeReference, neededDatum, ec);
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

    private EndpointActionResponse[] processEndpointActions(Detail detail, EntityScreen nextScreen) {
        DetailField[] fields = detail.getFields();
        EndpointActionResponse[] endpointActions = new EndpointActionResponse[fields.length];
        for (int i = 0; i < fields.length; i++) {
            DetailField field = fields[i];
            EndpointAction endpointAction = field.getEndpointAction();
            if (endpointAction != null) {
                String endpointId = endpointAction.getEndpointId();
                Endpoint endpoint = nextScreen.getEndpoint(endpointId);
                if (endpoint == null) {
                    throw new RuntimeException(
                            "Invalid endpoint_action id " + endpointId + " defined for case list screen");
                }
                String urlTemplate = getEndpointUrlTemplate(endpoint);
                endpointActions[i] = new EndpointActionResponse(urlTemplate, endpointAction.isBackground());
            }
        }
        return endpointActions;
    }

    private String getEndpointUrlTemplate(Endpoint endpoint) {
        StringBuilder urlTemplateBuilder = new StringBuilder("/a/{domain}/app/v1/{appid}/");
        urlTemplateBuilder.append(endpoint.getId());
        urlTemplateBuilder.append("/?");
        for (EndpointArgument argument : endpoint.getArguments()) {
            String arg = argument.getId();
            urlTemplateBuilder.append(arg);
            urlTemplateBuilder.append("={");
            urlTemplateBuilder.append(arg);
            urlTemplateBuilder.append("}");
        }
        return urlTemplateBuilder.toString();
    }

    private static DisplayElement[] processActions(SessionWrapper session, Vector<Action> actions) {
        ArrayList<DisplayElement> displayActions = new ArrayList<>();
        for (Action action : actions) {
            displayActions.add(new DisplayElement(action, session.getEvaluationContext()));
        }
        DisplayElement[] ret = new DisplayElement[actions.size()];
        displayActions.toArray(ret);
        return ret;
    }

    private static String processRedoLast(Vector<Action> entityListActions) {
        return formatActionsWithFilter(entityListActions, Action::isRedoLast);
    }

    private static String formatActionsWithFilter(Vector<Action> entityListActions,
            Predicate<Action> actionFilter) {
        String ret = null;
        int index = 0;
        for (Action action : entityListActions) {
            if (actionFilter.test(action)) {
                ret = "action " + index;
            }
            index = index + 1;
        }
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

    public String getRedoLast() {
        return redoLast;
    }

    public void setRedoLast(String redoLast) {
        this.redoLast = redoLast;
    }

    @Override
    public String toString() {
        return "EntityListResponse [Title= " + getTitle() +
                ", noItemsText=" + getNoItemsText() +
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

    public EndpointActionResponse[] getEndpointActions() {
        return endpointActions;
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

    public boolean isMultiSelect() {
        return isMultiSelect;
    }

    public void setMultiSelect(boolean multiSelect) {
        isMultiSelect = multiSelect;
    }

    public int getMaxSelectValue() {
        return maxSelectValue;
    }

    public void setMaxSelectValue(int maxSelectValue) {
        this.maxSelectValue = maxSelectValue;
    }

    private void setNoItemsText(String noItemsText) {
        this.noItemsText = noItemsText;
    }

    public String getNoItemsText() {
        return noItemsText;
    }

    private String getNoItemsTextLocaleString(Detail detail) {
        String noItemsTextString;
        try {
            noItemsTextString = detail.getNoItemsText().evaluate();
        } catch (NoLocalizedTextException | NullPointerException e) {
            return null;
        }
        return noItemsTextString;
    }

    public QueryResponseBean getQueryResponse() {
        return queryResponse;
    }

    public int getGroupHeaderRows() {
        return groupHeaderRows;
    }
}
