package repo.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import repo.TokenRepo;

import java.io.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * Created by willpride on 1/19/16.
 */
public class PostgresTokenRepo implements TokenRepo{

    private final Log log = LogFactory.getLog(PostgresTokenRepo.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public boolean isAuthorized(String tokenId) {
        log.info("Searching for token " + tokenId);
        String sql = "SELECT * FROM django_session WHERE session_key = ?";
        SessionToken token = jdbcTemplate.queryForObject(sql, new Object[] {tokenId}, new TokenMapper());
        if(token != null){
            return token.getExpireDate().after(new java.util.Date());
        }
        return false;
    }

    private static final class TokenMapper implements RowMapper<SessionToken> {

        public SessionToken mapRow(ResultSet rs, int rowNum) throws SQLException {

            SessionToken token = new SessionToken();
            token.setSessionId(rs.getString("session_key"));
            token.setExpireDate(rs.getDate("expire_date"));
            return token;
        }
    }

    private static class SessionToken {
        private String sessionId;
        private Date expireDate;

        public String getSessionid() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public Date getExpireDate() {
            return expireDate;
        }

        public void setExpireDate(Date expireDate) {
            this.expireDate = expireDate;
        }
    }


}
