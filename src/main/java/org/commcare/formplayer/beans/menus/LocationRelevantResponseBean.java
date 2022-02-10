package org.commcare.formplayer.beans.menus;

/**
 * Extended by response beans that may need to convey to the browser that the browser location is needed for a
 * function evaluation.
 * <p>
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

    public void setShouldRequestLocation(boolean b) {
        this.shouldRequestLocation = b;
    }

    public void setShouldWatchLocation(boolean b) {
        this.shouldWatchLocation = b;
    }

}
