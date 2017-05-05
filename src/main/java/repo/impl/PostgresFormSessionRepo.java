package repo.impl;

import exceptions.FormNotFoundException;
import objects.FunctionHandler;
import objects.SerializableFormSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import repo.FormSessionRepo;
import util.Constants;

import javax.persistence.LockModeType;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Postgres implementation for storing form entry sessions
 * Corresponds to the new_formplayer_session table in the formplayer database
 */
@Repository
public class PostgresFormSessionRepo implements FormSessionRepo {

    @Autowired
    @Qualifier("formplayerTemplate")
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<SerializableFormSession> findUserSessions(String username) {
        List<SerializableFormSession> sessions = this.jdbcTemplate.query(
                replaceTableName(
                        "SELECT *, dateopened::timestamptz as dateopened_timestamp " +
                        "FROM %s WHERE username = ? ORDER BY dateopened_timestamp DESC"
                ),
                new Object[] {username},
                new SessionMapper());
        return sessions;
    }

    @Override
    @Lock(LockModeType.OPTIMISTIC)
    public <S extends SerializableFormSession> Iterable<S> save(Iterable<S> entities) {
        for(SerializableFormSession session: entities){
            save(session);
        }
        return entities;
    }

    private byte[] writeToBytes(Object object){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
        } catch(IOException e){
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    @Override
    @Lock(LockModeType.OPTIMISTIC)
    public <S extends SerializableFormSession> S save(S session) {

        byte[] sessionDataBytes = writeToBytes(session.getSessionData());
        byte[] functionContextBytes = writeToBytes(session.getFunctionContext());

        int sessionCount = this.jdbcTemplate.queryForObject(
                replaceTableName("select count(*) from %s where id = ?"), Integer.class, session.getId());

        if(sessionCount > 0){
            String query = replaceTableName("UPDATE %s SET instanceXml = ?, sessionData = ?, " +
                    "sequenceId = ?, currentIndex = ?, postUrl = ? WHERE id = ?");
            this.jdbcTemplate.update(query,
                    new Object[] {
                            session.getInstanceXml(),
                            sessionDataBytes,
                            session.getSequenceId(),
                            session.getCurrentIndex(),
                            session.getPostUrl(),
                            session.getId()
                    },
                    new int[] {
                            Types.VARCHAR,
                            Types.BINARY,
                            Types.VARCHAR,
                            Types.VARCHAR,
                            Types.VARCHAR,
                            Types.VARCHAR
                    }
            );
            return session;
        }

        String query = replaceTableName("INSERT into %s " +
                "(id, instanceXml, formXml, " +
                "username, initLang, sequenceId, " +
                "domain, postUrl, sessionData, menu_session_id," +
                "title, dateOpened, oneQuestionPerScreen, currentIndex, asUser, appid, functioncontext) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        this.jdbcTemplate.update(
                query,
                new Object[] {
                        session.getId(),
                        session.getInstanceXml(),
                        session.getFormXml(),
                        session.getUsername(),
                        session.getInitLang(),
                        session.getSequenceId(),
                        session.getDomain(),
                        session.getPostUrl(),
                        sessionDataBytes,
                        session.getMenuSessionId(),
                        session.getTitle(),
                        session.getDateOpened(),
                        session.getOneQuestionPerScreen(),
                        session.getCurrentIndex(),
                        session.getAsUser(),
                        session.getAppId(),
                        functionContextBytes
                },
                new int[] {
                        Types.VARCHAR,
                        Types.VARCHAR,
                        Types.VARCHAR,
                        Types.VARCHAR,
                        Types.VARCHAR,
                        Types.VARCHAR,
                        Types.VARCHAR,
                        Types.VARCHAR,
                        Types.BINARY,
                        Types.VARCHAR,
                        Types.VARCHAR,
                        Types.VARCHAR,
                        Types.BOOLEAN,
                        Types.VARCHAR,
                        Types.VARCHAR,
                        Types.VARCHAR,
                        Types.BINARY
                }
        );
        return session;
    }

    public SerializableFormSession findOneWrapped(String id) throws FormNotFoundException {
        try {
            return findOne(id);
        } catch(EmptyResultDataAccessException e) {
            throw new FormNotFoundException(id);
        }
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
    @Lock(LockModeType.OPTIMISTIC)
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
    private final class SessionMapper implements RowMapper<SerializableFormSession> {

        public SerializableFormSession mapRow(ResultSet rs, int rowNum) throws SQLException {

            SerializableFormSession session = new SerializableFormSession();
            session.setId(rs.getString("id"));
            session.setInstanceXml(rs.getString("instanceXml"));
            session.setFormXml(rs.getString("formXml"));
            session.setUsername(rs.getString("username"));
            session.setInitLang(rs.getString("initLang"));
            session.setSequenceId(Integer.parseInt(rs.getString("sequenceId")));
            session.setDomain(rs.getString("domain"));
            session.setPostUrl(rs.getString("postUrl"));
            session.setMenuSessionId(rs.getString("menu_session_id"));
            session.setTitle(rs.getString("title"));
            session.setDateOpened(rs.getString("dateOpened"));
            session.setOneQuestionPerScreen(rs.getBoolean("oneQuestionPerScreen"));
            session.setCurrentIndex(rs.getString("currentIndex"));
            session.setAsUser(rs.getString("asUser"));
            session.setAppId(rs.getString("appid"));

            byte[] st = (byte[]) rs.getObject("sessionData");
            if (st != null) {
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

            byte[] fc = (byte[]) rs.getObject("functionContext");
            if (fc != null) {
                ByteArrayInputStream byteInputStream = new ByteArrayInputStream(fc);
                ObjectInputStream objectInputStream;
                try {
                    objectInputStream = new ObjectInputStream(byteInputStream);
                    Map<String, FunctionHandler[]> functionContext = (HashMap) objectInputStream.readObject();
                    session.setFunctionContext(functionContext);
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
