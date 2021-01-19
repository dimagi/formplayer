package org.commcare.formplayer.postgresutil;

/**
 * @author $|-|!˅@M
 */
public class PostgresApplicationDB extends PostgresDB {

    public PostgresApplicationDB(String domain, String username, String asUsername, String appId) {
        super(new PostgresDBPath(domain, username, asUsername, appId));
    }

}
