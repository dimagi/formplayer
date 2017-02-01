package beans;

/**
 * Created by willpride on 1/20/16.
 */
public class RunQARequestBean extends AuthenticatedRequestBean {
    private String qaPlan;

    // default constructor for Jackson
    public RunQARequestBean(){}

    public String getQaPlan() {
        return qaPlan;
    }

    public void setQaPlan(String qaPlan) {
        this.qaPlan = qaPlan;
    }
}
