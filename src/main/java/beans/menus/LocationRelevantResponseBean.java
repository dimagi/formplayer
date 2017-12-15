package beans.menus;

import session.MenuSession;

/**
 * Extended by response beans that may need to convey to the browser that the browser location is needed for a
 * function evaluation.
 *
 * Created by amstone326 on 12/13/17.
 */
public abstract class LocationRelevantResponseBean {

    protected boolean shouldRequestLocation;
    protected boolean shouldWatchLocation;

    public boolean getShouldRequestLocation() {
        return this.shouldRequestLocation;
    }

    public boolean getShouldWatchLocation() {
        return this.shouldWatchLocation;
    }

    public static <T extends LocationRelevantResponseBean> T setLocationNeeds(T responseBean, MenuSession menuSession) {
        responseBean.shouldRequestLocation = menuSession.locationRequestNeeded();
        responseBean.shouldWatchLocation = menuSession.hereFunctionEvaluated();
        return responseBean;
    }

}