package beans.menus;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.util.screen.EntityDetailSubscreen;
import org.commcare.util.screen.EntityListSubscreen;
import org.commcare.util.screen.EntityScreen;
import org.commcare.util.screen.Subscreen;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

import java.util.ArrayList;

/**
 * Created by willpride on 1/4/17.
 */
public class EntityDetailListResponse {

    private EntityDetailResponse[] entityDetailList;
    private boolean isPersistentDetail;

    public EntityDetailListResponse() {
    }

    public EntityDetailListResponse(EntityDetailResponse entityDetailResponse) {
        this.entityDetailList = new EntityDetailResponse[]{entityDetailResponse};
        this.isPersistentDetail = true;
    }

    public EntityDetailListResponse(EntityScreen screen, EvaluationContext ec, TreeReference treeReference) {
        entityDetailList = processDetails(screen, ec, treeReference);
    }

    private EntityDetailResponse[] processDetails(EntityScreen screen, EvaluationContext ec, TreeReference ref) {
        Detail[] detailList = screen.getLongDetailList();
        if (detailList == null || !(detailList.length > 0)) {
            // No details, just return null
            return null;
        }
        EvaluationContext subContext = new EvaluationContext(ec, ref);
        ArrayList<Object> accumulator = new ArrayList<>();
        for (int i = 0; i < detailList.length; i++) {
            // For now, don't add sub-details
            if (detailList[i].getNodeset() == null) {
                EntityDetailSubscreen subscreen = new EntityDetailSubscreen(i,
                        detailList[i],
                        subContext,
                        screen.getDetailListTitles(subContext));
                EntityDetailResponse response = new EntityDetailResponse(subscreen, screen.getDetailListTitles(subContext)[i]);
                accumulator.add(response);
            } else {
                TreeReference contextualizedNodeset = detailList[i].getNodeset().contextualize(ref);
                EntityDetailResponse response = new EntityDetailResponse(detailList[i],
                        subContext.expandReference(contextualizedNodeset),
                        subContext,
                        (EntityDatum) screen.getSession().getNeededDatum(),
                        screen.getDetailListTitles(subContext)[i]);
                accumulator.add(response);
            }
        }
        EntityDetailResponse[] ret = new EntityDetailResponse[accumulator.size()];
        accumulator.toArray(ret);
        return ret;
    }

    public EntityDetailListResponse(Detail[] detailList, SessionDatum datum,
                                    EvaluationContext ec, TreeReference treeReference) {
        entityDetailList = processDetails(detailList, datum, ec, treeReference);
    }

    private EntityDetailResponse[] processDetails(Detail[] detailList,
                                                  SessionDatum neededDatum,
                                                  EvaluationContext ec,
                                                  TreeReference ref) {

        if (detailList == null || !(detailList.length > 0)) {
            // No details, just return null
            return null;
        }

        String[] titles = new String[detailList.length];
        for (int i = 0; i < detailList.length; ++i) {
            titles[i] = detailList[i].getTitle().getText().evaluate(ec);
        }

        EvaluationContext subContext = new EvaluationContext(ec, ref);
        ArrayList<Object> accumulator = new ArrayList<>();
        for (int i = 0; i < detailList.length; i++) {
            // For now, don't add sub-details
            if (detailList[i].getNodeset() == null) {
                EntityDetailSubscreen subscreen = new EntityDetailSubscreen(i,
                        detailList[i],
                        subContext,
                        titles);
                EntityDetailResponse response = new EntityDetailResponse(subscreen, titles[i]);
                accumulator.add(response);
            } else {
                TreeReference contextualizedNodeset = detailList[i].getNodeset().contextualize(ref);
                EntityDetailResponse response = new EntityDetailResponse(detailList[i],
                        subContext.expandReference(contextualizedNodeset),
                        subContext,
                        (EntityDatum) neededDatum,
                        titles[i]);
                accumulator.add(response);
            }
        }
        EntityDetailResponse[] ret = new EntityDetailResponse[accumulator.size()];
        accumulator.toArray(ret);
        return ret;
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
}
