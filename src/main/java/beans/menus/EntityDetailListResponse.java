package beans.menus;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.commcare.suite.model.Detail;
import org.commcare.util.screen.EntityDetailSubscreen;
import org.commcare.util.screen.EntityScreen;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

/**
 * Created by willpride on 1/4/17.
 */
public class EntityDetailListResponse {

    private EntityDetailResponse[] entityDetailList;

    public EntityDetailListResponse() {}

    public EntityDetailListResponse(EntityScreen screen, EvaluationContext ec, TreeReference treeReference) {
        EntityDetailSubscreen[] subscreens = processDetails(screen, ec, treeReference);
        if (subscreens != null) {
            EntityDetailResponse[] responses = new EntityDetailResponse[subscreens.length];
            for (int j = 0; j < subscreens.length; j++) {
                responses[j] = new EntityDetailResponse(subscreens[j]);
                responses[j].setTitle(subscreens[j].getTitles()[j]);
            }
            entityDetailList = responses;
        } else {
            entityDetailList = null;
        }
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

    @JsonGetter(value = "details")
    public EntityDetailResponse[] getEntityDetailList() {
        return entityDetailList;
    }

    @JsonSetter(value = "details")
    public void setEntityDetailList(EntityDetailResponse[] entityDetailList) {
        this.entityDetailList = entityDetailList;
    }
}
