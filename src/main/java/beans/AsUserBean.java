package beans;

/**
 * Implementers of this interface can restore a different user than the
 * one they are authenticated as
 */
public interface AsUserBean {
    public String getAsUser();
    public String getUsername();
    public String getDomain();
}
