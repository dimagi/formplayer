package beans.debugger;

/**
 * Created by benrudolph on 11/27/16.
 */
public class XPathQueryItem {
    private String xpath;
    private String output;
    private String status;

    public XPathQueryItem(String xpath, String output, String status) {
        this.xpath = xpath;
        this.output = output;
        this.status = status;
    }

    public XPathQueryItem() {}

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
