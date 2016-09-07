package repo.impl;

import hq.models.PostgresUser;
import hq.models.SessionToken;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import util.Constants;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by benrudolph on 9/7/16.
 */
@Repository
public class PostgresUserRepo {
    private final Log log = LogFactory.getLog(PostgresTokenRepo.class);

    @Autowired
    @Qualifier("hqTemplate")
    private JdbcTemplate jdbcTemplate;

    public PostgresUser getUserByDjangoId(int userId) {
        String sql = String.format("SELECT * FROM %s WHERE id = ?",
                Constants.POSTGRES_USER_TABLE_NAME);
        return jdbcTemplate.queryForObject(sql, new Object[] {userId}, new PostgresUserMapper());
    }

    private static final class PostgresUserMapper implements RowMapper<PostgresUser> {

        public PostgresUser mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PostgresUser(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getBoolean("is_superuser")
            );
        }
    }
}
