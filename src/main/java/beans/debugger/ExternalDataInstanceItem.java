package beans.debugger;

/**
 * Created by willpride on 11/15/16.
 */
public class ExternalDataInstanceItem {
    private String instanceName;
    private String instanceXml;

    public ExternalDataInstanceItem(String instanceName, String instanceXml) {
        this.instanceName = instanceName;
        this.instanceXml = instanceXml;
    }

    public String getInstanceXml() {
        return instanceXml;
    }

    public void setInstanceXml(String instanceXml) {
        this.instanceXml = instanceXml;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }
}
