package org.commcare.formplayer.beans.debugger;

/**
 * Created by benrudolph on 6/1/17.
 */
public class InstanceAutocompletableItem extends AutoCompletableItem {

    private final String CASEDB_INSTANCE = "casedb";
    private final String SESSION_INSTANCE = "commcaresession";
    private final String LOCATION_INSTANCE = "locations";
    private final String REPORTS_INSTANCE = "commcare:reports";

    public InstanceAutocompletableItem(String instanceId) {
        super(null, null, null);
        String instance;
        String baseInstance = "instance('" + instanceId + "')/";

        switch (instanceId) {
            case CASEDB_INSTANCE:
                instance = baseInstance + "casedb/case";
                break;
            case SESSION_INSTANCE:
                instance = baseInstance + "org/commcare/formplayer/session";
                break;
            case LOCATION_INSTANCE:
                instance = baseInstance + "locations";
                break;
            case REPORTS_INSTANCE:
                instance = baseInstance + "reports";
                break;
            default:
                instance = baseInstance;
        }
        this.value = instance;
        this.label = instanceId;
        this.type = "Instance";
    }
}
