package repo.impl;

import objects.SerializableFormSession;
import objects.SessionData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import repo.SessionRepo;
import util.Constants;

import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Postgres implementation for storing form entry sessions
 * Corresponds to the new_formplayer_session table in the formplayer database
 */
@Repository
public class PostgresSessionRepo implements SessionRepo{

    @Autowired
    @Qualifier("formplayerTemplate")
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<SerializableFormSession> findUserSessions(String username) {
        List<SerializableFormSession> sessions = this.jdbcTemplate.query(
                replaceTableName("SELECT * FROM %s WHERE username = ?"),
                new Object[] {username},
                new SessionMapper());
        return sessions;
    }

    @Override
    public <S extends SerializableFormSession> Iterable<S> save(Iterable<S> entities) {
        for(SerializableFormSession session: entities){
            save(session);
        }
        return entities;
    }

    @Override
    public <S extends SerializableFormSession> S save(S session) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(session.getSessionData());
        } catch(IOException e){
            throw new RuntimeException(e);
        }
        byte[] sessionDataBytes = baos.toByteArray();

        int sessionCount = this.jdbcTemplate.queryForObject(
                replaceTableName("select count(*) from %s where id = ?"), Integer.class, session.getId());

        if(sessionCount > 0){
            String query = replaceTableName("UPDATE %s SET instanceXml = ?, sessionData = ? WHERE id = ?");
            this.jdbcTemplate.update(query,  new Object[] {session.getInstanceXml(), sessionDataBytes, session.getId()},
                    new int[] {Types.VARCHAR, Types.BINARY, Types.VARCHAR});
            return session;
        }

        String query = replaceTableName("INSERT into %s " +
                "(id, instanceXml, formXml, " +
                "restoreXml, username, initLang, sequenceId, " +
                "domain, postUrl, sessionData) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        this.jdbcTemplate.update(query,  new Object[] {session.getId(), session.getInstanceXml(), session.getFormXml(),
                session.getRestoreXml(), session.getUsername(), session.getInitLang(), session.getSequenceId(),
                session.getDomain(), session.getPostUrl(), sessionDataBytes}, new int[] {
                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BINARY});
        return session;
    }

    @Override
    public SerializableFormSession findOne(String id) {
        String sql = replaceTableName("SELECT * FROM %s WHERE id = ?");
        return jdbcTemplate.queryForObject(sql, new Object[] {id}, new SessionMapper());
    }

    @Override
    public boolean exists(String s) {
        String sql = replaceTableName("select exists(select 1 from %s where id = ?)");
        return this.jdbcTemplate.queryForObject(sql, boolean.class, s);
    }

    @Override
    public Iterable<SerializableFormSession> findAll() {
        List<SerializableFormSession> sessions = this.jdbcTemplate.query(
                replaceTableName("SELECT * FROM %s"),
                new SessionMapper());
        return sessions;
    }

    @Override
    public Iterable<SerializableFormSession> findAll(Iterable<String> strings) {
        List<SerializableFormSession> sessions = this.jdbcTemplate.query(
                replaceTableName("SELECT * FROM %s WHERE id in(SELECT * FROM UNNEST (?)"),
                new Object[] {strings},
                new SessionMapper());
        return sessions;
    }

    @Override
    public long count() {
        return this.jdbcTemplate.queryForObject(
                replaceTableName("select count(*) from %s"), Integer.class);
    }

    @Override
    public void delete(String id) {
        this.jdbcTemplate.update(replaceTableName("DELETE FROM %s WHERE id = ?"), id);
    }

    @Override
    public void delete(SerializableFormSession entity) {
        delete(entity.getId());
    }

    @Override
    public void delete(Iterable<? extends SerializableFormSession> entities) {
        for(SerializableFormSession session: entities){
            delete(session.getId());
        }
    }

    @Override
    public void deleteAll() {
        Iterable<SerializableFormSession> allSessions = findAll();
        for(SerializableFormSession session: allSessions){
            delete(session.getId());
        }
    }

    // helper class for mapping a db row to a serialized session
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
                    throw new SQLException(e);
                } catch (ClassNotFoundException e) {
                    throw new SQLException(e);
                }
            }

            return session;
        }
    }

    public String replaceTableName(String query){
        return String.format(query, Constants.POSTGRES_SESSION_TABLE_NAME);
    }

}
