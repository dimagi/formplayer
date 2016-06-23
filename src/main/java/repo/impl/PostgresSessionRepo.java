package repo.impl;

import objects.SerializableFormSession;
import objects.SessionData;
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

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(session.getSessionData());
            byte[] sessionDataBytes = baos.toByteArray();

            int sessionCount = this.jdbcTemplate.queryForObject(
                    "select count(*) from sessions where id = ?", Integer.class, session.getId());

            if(sessionCount > 0){
                String query = "UPDATE sessions SET instanceXml = ?, sessionData = ? WHERE id = ?";
                this.jdbcTemplate.update(query,  new Object[] {session.getInstanceXml(), sessionDataBytes, session.getId()},
                        new int[] {Types.VARCHAR, Types.BINARY, Types.VARCHAR});
                return;
            }

            System.out.println("Saving Session: " + session.getRestoreXml());

            String query = "INSERT into sessions (id, instanceXml, formXml, " +
                    "restoreXml, username, initLang, sequenceId, " +
                    "domain, postUrl, sessionData) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            this.jdbcTemplate.update(query,  new Object[] {session.getId(), session.getInstanceXml(), session.getFormXml(),
                    session.getRestoreXml(), session.getUsername(), session.getInitLang(), session.getSequenceId(),
                    session.getDomain(), session.getPostUrl(), sessionDataBytes}, new int[] {
                    Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                    Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BINARY});
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    public List<SerializableFormSession> findUserSessions(String username) {
        List<SerializableFormSession> sessions = this.jdbcTemplate.query(
                "SELECT * FROM sessions WHERE username = ?",
                new Object[] {username},
                new SessionMapper());
        return sessions;
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

            byte[] st = (byte[]) rs.getObject("sessionData");

            if(st != null) {
                ByteArrayInputStream byteInputStream = new ByteArrayInputStream(st);
                ObjectInputStream objectInputStream;
                try {
                    objectInputStream = new ObjectInputStream(byteInputStream);
                    Map<String, String> sessionData = (HashMap) objectInputStream.readObject();
                    session.setSessionData(sessionData);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            return session;
        }
    }

}
