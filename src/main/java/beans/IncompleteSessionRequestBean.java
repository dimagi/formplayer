package beans;

/**
 * Request to open an incomplete form session (starts form entry)
 */
public class IncompleteSessionRequestBean extends AuthenticatedRequestBean implements AsUserBean{
    private String sessionId;
    private String asUser;

    public IncompleteSessionRequestBean (){}

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String getAsUser() {
        return asUser;
    }

    public void setAsUser(String asUser) {
        this.asUser = asUser;
    }
}
