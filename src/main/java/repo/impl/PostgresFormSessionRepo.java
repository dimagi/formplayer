package repo.impl;

import exceptions.FormNotFoundException;
import objects.SerializableFormSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import repo.FormSessionRepo;
import session.FormSession;
import util.Constants;

import javax.persistence.LockModeType;
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

    private class FormSessionStatementSetter implements PreparedStatementSetter {
        private SerializableFormSession mSession;

        public FormSessionStatementSetter(SerializableFormSession session) {
            mSession = session;
        }

        @Override
        public void setValues(PreparedStatement ps) throws SQLException {
            byte[] sessionDataBytes = writeToBytes(mSession.getSessionData());

            ps.setString(1, mSession.getInstanceXml());
            ps.setString(2, mSession.getId());
            ps.setString(3, mSession.getFormXml());
            try {
                ps.setCharacterStream(4, new InputStreamReader(mSession.getRestoreXml(), "UTF-8"), mSession.getRestoreXml().available());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ps.setString(5, mSession.getUsername());
            ps.setString(6, mSession.getInitLang());
            ps.setString(7, String.valueOf(mSession.getSequenceId()));
            ps.setString(8, mSession.getDomain());
            ps.setString(9, mSession.getPostUrl());
            ps.setBytes(10, sessionDataBytes);
            ps.setString(11, mSession.getMenuSessionId());
            ps.setString(12, mSession.getTitle());
            ps.setString(13, mSession.getDateOpened());
            ps.setBoolean(14, mSession.getOneQuestionPerScreen());
            ps.setString(15, mSession.getCurrentIndex());
            ps.setString(16, mSession.getAsUser());
            ps.setString(17, mSession.getAppId());
        }
    }

    private class FormSessionUpdateStatementSetter implements PreparedStatementSetter {
        private SerializableFormSession mSession;

        public FormSessionUpdateStatementSetter(SerializableFormSession session) {
            mSession = session;
        }

        @Override
        public void setValues(PreparedStatement ps) throws SQLException {
            byte[] sessionDataBytes = writeToBytes(mSession.getSessionData());

            ps.setString(0, mSession.getInstanceXml());
            ps.setBytes(1, sessionDataBytes);
            ps.setString(2, String.valueOf(mSession.getSequenceId()));
            ps.setString(3, mSession.getCurrentIndex());
            ps.setString(4, mSession.getPostUrl());
            try {
                ps.setCharacterStream(5, new InputStreamReader(mSession.getRestoreXml(), "UTF-8"), mSession.getRestoreXml().available());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ps.setString(6, mSession.getId());
        }
    }

    @Override
    @Lock(LockModeType.OPTIMISTIC)
    public <S extends SerializableFormSession> S save(S session) {

        int sessionCount = this.jdbcTemplate.queryForObject(
                replaceTableName("select count(*) from %s where id = ?"), Integer.class, session.getId());

        if(sessionCount > 0){
            String query = replaceTableName("UPDATE %s SET instanceXml = ?, sessionData = ?, " +
                    "sequenceId = ?, currentIndex = ?, postUrl = ?, restoreXml = ? WHERE id = ?");

            this.jdbcTemplate.update(
                    query,
                    new FormSessionUpdateStatementSetter(session)
            );
            return session;
        }

        String query = replaceTableName("INSERT into %s " +
                "(id, instanceXml, formXml, " +
                "restoreXml, username, initLang, sequenceId, " +
                "domain, postUrl, sessionData, menu_session_id," +
                "title, dateOpened, oneQuestionPerScreen, currentIndex, asUser, appid) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        this.jdbcTemplate.update(query, new FormSessionStatementSetter(session));
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
            Blob blobXml = rs.getBlob("restoreXml");
            InputStream restoreStream = blobXml.getBinaryStream();
            session.setRestoreXml(restoreStream);
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
            return session;
        }
    }

    public String replaceTableName(String query){
        return String.format(query, Constants.POSTGRES_SESSION_TABLE_NAME);
    }

}
