package hq.models;

/**
 * Created by benrudolph on 9/7/16.
 */
public class PostgresUser {

    private String username;
    private int userId;
    private boolean isSuperuser;

    public PostgresUser(int userId, String username, boolean isSuperuser) {
        this.userId = userId;
        this.username = username;
        this.isSuperuser = isSuperuser;
    }

    public String getUsername() {
        return username;
    }

    public int getUserId() {
        return userId;
    }

    public boolean isSuperuser() {
        return isSuperuser;
    }
}
