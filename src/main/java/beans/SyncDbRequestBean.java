package beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * I think we can delete this now  that all information has been encapsulated in its super class
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncDbRequestBean extends AuthenticatedRequestBean {
    private boolean preserveCache;

    public SyncDbRequestBean(){}

    public boolean isPreserveCache() {
        return preserveCache;
    }

    public void setPreserveCache(boolean preserveCache) {
        this.preserveCache = preserveCache;
    }
}
