package beans;

/**
 * Request to open an incomplete form session (starts form entry)
 */
public class IncompleteSessionRequestBean extends AuthenticatedRequestBean implements AsUserBean{
    private String sessionId;

    public IncompleteSessionRequestBean (){}

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String getAsUser() {
        return null;
    }
}
