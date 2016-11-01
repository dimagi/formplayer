package beans;

/**
 * POST request for getting a list of the user's incomplete form sessions
 */
public class GetSessionsBean implements AsUserBean {
    private String username;
    private String domain;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public String getAsUser() {
        return null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
