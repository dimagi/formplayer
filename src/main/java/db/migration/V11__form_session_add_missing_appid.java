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
 * Attempt to fix null appId in form session
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
                    String menuId = rs.getString("menu_session_id");
                    FixObject obj = new FixObject(id, postUrl, menuId);
                    toBeFixed.add(obj);
                }
                return toBeFixed;
            }});

        toBeFixed.forEach(new Consumer() {
            @Override
            public void accept(Object o) {
                FixObject fixObject = (FixObject) o;
                String sessionId = fixObject.getId();
                String menuSessionId = fixObject.getMenuId();

                if (menuSessionId != null && !"".equals(menuSessionId)) {
                    String sql = String.format("SELECT appid FROM menu_sessions WHERE id = ?");
                    String menuAppId = jdbcTemplate.queryForObject(sql, new Object[] { menuSessionId }, String.class);
                    if (menuAppId != null && !"".equals(menuAppId)) {
                        executeUpdate(jdbcTemplate, menuAppId, sessionId);
                        return;
                    }
                }
                String postAppId = fixObject.getPostUrlAppId();
                if (postAppId != null && !"".equals(postAppId)) {
                    executeUpdate(jdbcTemplate, postAppId, sessionId);
                }
            }
        });
    }

    private void executeUpdate(JdbcTemplate jdbcTemplate, String appId, String sessionId) {
        jdbcTemplate.execute(String.format("UPDATE formplayer_sessions SET appId = '%s' WHERE id = '%s'", appId, sessionId));
    }

    class FixObject {
        private String id;
        private String postUrl;
        private String menuId;

        public FixObject (String id, String postUrl, String menuId) {
            this.id = id;
            this.postUrl = postUrl;
            this.menuId = menuId;
        }

        public String getPostUrlAppId() {

            if (postUrl == null || postUrl.equals("")) {
                return null;
            }
            String appId = postUrl;
            if (appId.endsWith("/")) {
                appId = appId.substring(0, appId.length() - 1);
            }
            int lastSlashIndex = appId.lastIndexOf('/');
            appId = appId.substring(lastSlashIndex + 1);
            return appId;
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

        public String getMenuId() {
            return menuId;
        }

        public void setMenuId(String menuId) {
            this.menuId = menuId;
        }
    }
}
