package repo.impl;

import objects.SerializableFormSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import repo.SessionRepo;

import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by willpride on 1/19/16.
 */
public class PostgresSessionRepo implements SessionRepo{

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void save(SerializableFormSession session) {

        int countOfActorsNamedJoe = this.jdbcTemplate.queryForObject(
                "select count(*) from sessions where id = ?", Integer.class, session.getId());

        if(countOfActorsNamedJoe > 0){
            String query = "UPDATE sessions SET instanceXml = ? WHERE id = ?";
            this.jdbcTemplate.update(query,  new Object[] {session.getInstanceXml(), session.getId()},
                    new int[] {Types.VARCHAR, Types.VARCHAR});
            return;
        }

        String query = "INSERT into sessions (id, instanceXml, formXml, " +
                "restoreXml, username, initLang, sequenceId, " +
                "domain, postUrl) VALUES " +
                "(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        this.jdbcTemplate.update(query,  new Object[] {session.getId(), session.getInstanceXml(), session.getFormXml(),
                session.getRestoreXml(), session.getUsername(), session.getInitLang(), session.getSequenceId(),
                session.getDomain(), session.getPostUrl()}, new int[] {
                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR});
    }

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public SerializableFormSession find(String id) {
        String sql = "SELECT * FROM sessions WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[] {id}, new SessionMapper());
    }

    @Override
    public Map<Object, Object> findAll() {
        List<SerializableFormSession> sessions = this.jdbcTemplate.query(
                "SELECT * FROM sessions",
                new SessionMapper());
        Map<Object, Object> ret = new HashMap<>();
        for (SerializableFormSession session : sessions){
            ret.put(session.getId(), session);
        }
        return ret;
    }

    @Override
    public void delete(String id) {
        this.jdbcTemplate.update("DELETE FROM sessions WHERE id = ?", Long.valueOf(id));
    }

    private static final class SessionMapper implements RowMapper<SerializableFormSession> {

        public SerializableFormSession mapRow(ResultSet rs, int rowNum) throws SQLException {
            SerializableFormSession session = new SerializableFormSession();
            session.setId(rs.getString("id"));
            session.setInstanceXml(rs.getString("instanceXml"));
            session.setFormXml(rs.getString("formXml"));
            session.setRestoreXml(rs.getString("restoreXml"));
            session.setUsername(rs.getString("username"));
            session.setInitLang(rs.getString("initLang"));
            session.setSequenceId(Integer.parseInt(rs.getString("sequenceId")));
            session.setDomain(rs.getString("domain"));
            session.setPostUrl(rs.getString("postUrl"));
            return session;
        }
    }

}
