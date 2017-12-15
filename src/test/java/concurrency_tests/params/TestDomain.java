package concurrency_tests.params;

/**
 * Created by willpride on 4/11/17.
 */
public class TestDomain {
    private String domain;
    private String[] users;
    private TestApp[] apps;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String[] getUsers() {
        return users;
    }

    public void setUsers(String[] users) {
        this.users = users;
    }

    public TestApp[] getApps() {
        return apps;
    }

    public void setApps(TestApp[] apps) {
        this.apps = apps;
    }
}
