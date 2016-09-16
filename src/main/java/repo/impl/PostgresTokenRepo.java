package repo.impl;

import hq.models.SessionToken;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import repo.TokenRepo;
import util.Constants;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO implementation for HQ's django_session key table.
 */
@Repository
public class PostgresTokenRepo implements TokenRepo{

    private final Log log = LogFactory.getLog(PostgresTokenRepo.class);

    @Autowired
    @Qualifier("hqTemplate")
    private JdbcTemplate jdbcTemplate;

    @Override
    public SessionToken getSessionToken(String tokenId) {
        String sql = String.format("SELECT * FROM %s WHERE session_key = ? and expire_date > now()",
                Constants.POSTGRES_TOKEN_TABLE_NAME);
        SessionToken token = jdbcTemplate.queryForObject(sql, new Object[] {tokenId}, new TokenMapper());

        return token;
    }

    private static final class TokenMapper implements RowMapper<SessionToken> {

        public SessionToken mapRow(ResultSet rs, int rowNum) throws SQLException {

            SessionToken token = new SessionToken();
            token.setSessionId(rs.getString("session_key"));
            token.setExpireDate(rs.getDate("expire_date"));
            token.parseSessionData(rs.getString("session_data"));
            return token;
        }
    }




}
