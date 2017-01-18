package db.migration;

import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Add appId to form session
 */
public class V11__form_session_add_missing_appid implements SpringJdbcMigration {
    @Override
    public void migrate(final JdbcTemplate jdbcTemplate) throws Exception {
        String sql = "SELECT * FROM formplayer_sessions WHERE COALESCE(appid, '') = ''";
        List toBeFixed = jdbcTemplate.query(sql, new ResultSetExtractor<List>(){

            public List extractData(ResultSet rs) throws SQLException,
                    DataAccessException {
                List toBeFixed = new ArrayList();
                while(rs.next()) {
                    String id = rs.getString("id");
                    String postUrl = rs.getString("postUrl");
                    FixObject obj = new FixObject(id, postUrl);
                    toBeFixed.add(obj);
                }
                return toBeFixed;
            }});

        toBeFixed.forEach(new Consumer() {
            @Override
            public void accept(Object o) {
                FixObject fixObject = (FixObject) o;
                String appId = fixObject.getAppId();
                String id = fixObject.getId();
                System.out.println("Adding appId " + appId + " to id " + id);
                jdbcTemplate.execute(String.format("UPDATE formplayer_sessions SET appId = '%s' WHERE id = '%s'", appId, fixObject.getId()));
            }
        });
    }

    class FixObject {
        private String id;
        private String postUrl;

        public FixObject (String id, String postUrl) {
            this.id = id;
            this.postUrl = postUrl;
        }

        public String getAppId() {
            if (postUrl == null || postUrl.equals("")) {
                return "";
            }
            int startIndex = postUrl.indexOf("/receiver/") + "/receiver/".length();
            int endIndex = postUrl.endsWith("/") ? postUrl.length() - 1 : postUrl.length();
            return postUrl.substring(startIndex, endIndex);
        }


        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPostUrl() {
            return postUrl;
        }

        public void setPostUrl(String postUrl) {
            this.postUrl = postUrl;
        }
    }
}
