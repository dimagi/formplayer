package beans.debugger;

/**
 * Created by benrudolph on 6/1/17.
 */
public class InstanceAutocompletableItem extends AutoCompletableItem {

    private final String CASEDB_INSTANCE = "casedb";
    private final String SESSION_INSTANCE = "commcaresession";

    public InstanceAutocompletableItem(String instanceId) {
        super(null, null, null);
        String instance;
        String baseInstance = "instance('" + instanceId + "')/";

        switch (instanceId) {
            case CASEDB_INSTANCE:
                instance = baseInstance + "casedb/case[]";
                break;
            case SESSION_INSTANCE:
                instance = baseInstance + "session/";
                break;
            default:
                instance = baseInstance;
        }
        this.value = instance;
        this.label = instanceId;
        this.type = "Instance";
    }
}
