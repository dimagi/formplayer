package org.commcare.formplayer.beans.menus;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.commcare.suite.model.Detail;
import org.commcare.util.screen.EntityDetailSubscreen;
import org.commcare.util.screen.EntityScreen;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.ArrayList;

/**
 * Created by willpride on 1/4/17.
 */
public class EntityDetailListResponse extends LocationRelevantResponseBean {

    private EntityDetailResponse[] entityDetailList;
    private boolean isPersistentDetail;
    private String[] smartLinkParams;

    public EntityDetailListResponse() {}

    public EntityDetailListResponse(EntityDetailResponse entityDetailResponse) {
        this.entityDetailList = new EntityDetailResponse[]{entityDetailResponse};
        this.isPersistentDetail = true;
    }

    public EntityDetailListResponse(EntityScreen screen, EvaluationContext ec, TreeReference treeReference, boolean isFuzzySearchEnabled) {
        entityDetailList = processDetails(screen, ec, treeReference, isFuzzySearchEnabled);
    }

    public EntityDetailListResponse(Detail[] detailList, EvaluationContext ec, TreeReference treeReference, boolean isFuzzySearchEnabled) {
        entityDetailList = processDetails(detailList, ec, treeReference, isFuzzySearchEnabled);
    }

    private EntityDetailResponse[] processDetails(EntityScreen screen, EvaluationContext ec, TreeReference ref, boolean isFuzzySearchEnabled) {
        return processDetails(screen.getLongDetailList(ref), ec, ref, isFuzzySearchEnabled);
    }

    private EntityDetailResponse[] processDetails(Detail[] detailList,
                                                  EvaluationContext ec,
                                                  TreeReference ref,
                                                  boolean isFuzzySearchEnabled) {
        if (detailList == null || !(detailList.length > 0)) {
            // No details, just return null
            return null;
        }

        String[] titles = new String[detailList.length];
        for (int i = 0; i < detailList.length; ++i) {
            titles[i] = detailList[i].getTitle().getText().evaluate(ec);
        }

        EvaluationContext subContext = new EvaluationContext(ec, ref);
        setSmartLinkParams(subContext);

        ArrayList<Object> accumulator = new ArrayList<>();
        for (int i = 0; i < detailList.length; i++) {
            if (detailList[i].getNodeset() == null) {
                EntityDetailSubscreen subscreen = new EntityDetailSubscreen(i,
                        detailList[i],
                        subContext,
                        titles);
                EntityDetailResponse response = new EntityDetailResponse(subscreen, titles[i], detailList[i]);
                accumulator.add(response);
            } else {
                TreeReference contextualizedNodeset = detailList[i].getNodeset().contextualize(ref);
                EntityDetailResponse response = new EntityDetailResponse(detailList[i],
                        subContext.expandReference(contextualizedNodeset),
                        subContext,
                        titles[i],
                        isFuzzySearchEnabled);
                accumulator.add(response);
            }
        }
        EntityDetailResponse[] ret = new EntityDetailResponse[accumulator.size()];
        accumulator.toArray(ret);
        return ret;
    }

    private void setSmartLinkParams(EvaluationContext ec) {
        try {
            XPathExpression paramExpr = XPathParseTool.parseXPath("commcare_project");
            String commcare_project = (String) ((XPathNodeset) paramExpr.eval(ec)).unpack();
            if (!commcare_project.equals("")) {
                String[] params = new String[1];
                params[0] = commcare_project;
                this.setSmartLinkParams(params);
            }
        } catch (XPathSyntaxException e) {
            // nothing to do here
        }
    }

    @JsonGetter(value = "details")
    public EntityDetailResponse[] getEntityDetailList() {
        return entityDetailList;
    }

    @JsonSetter(value = "details")
    public void setEntityDetailList(EntityDetailResponse[] entityDetailList) {
        this.entityDetailList = entityDetailList;
    }

    @JsonGetter(value = "isPersistentDetail")
    public boolean getPersistentDetail() {
        return isPersistentDetail;
    }

    @JsonSetter(value = "isPersistentDetail")
    public void setPersistentDetail(boolean persistentDetail) {
        this.isPersistentDetail = persistentDetail;
    }

    @JsonGetter(value = "smartLinkParams")
    public String[] getSmartLinkParams() {
        return smartLinkParams;
    }

    @JsonSetter(value = "smartLinkParams")
    public void setSmartLinkParams(String[] params) {
        this.smartLinkParams = params;
    }
}
