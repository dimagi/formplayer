package beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * I think we can delete this now  that all information has been encapsulated in its super class
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncDbRequestBean extends AuthenticatedRequestBean {
    public SyncDbRequestBean(){}
}
