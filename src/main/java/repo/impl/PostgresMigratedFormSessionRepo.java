package repo.impl;

import exceptions.FormNotFoundException;
import objects.SerializableFormSession;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import repo.FormSessionRepo;
import util.Constants;
import util.SessionUtils;

import javax.persistence.LockModeType;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Postgres implementation for storing form entry sessions
 * Corresponds to the new_formplayer_session table in the formplayer database
 */
@Repository
public class PostgresMigratedFormSessionRepo implements FormSessionRepo {

    @Autowired
    @Qualifier("hqTemplate")
    private JdbcTemplate jdbcTemplate;

    public static final String POSTGRES_MIGRATED_SESSION_TABLE_NAME = "formplayer_session";
    public static final String POSTGRES_MIGRATED_ENTRYSESSION_TABLE_NAME = "formplayer_entrysession";
    public static final String POSTGRES_AUTH_USER_TABLE_NAME = "auth_user";

    @Override
    public List<SerializableFormSession> findUserSessions(String username) {

        List<Integer> userIds = this.jdbcTemplate.query(
                replaceTableName("SELECT * FROM %s WHERE username = ?", POSTGRES_AUTH_USER_TABLE_NAME),
                new Object[] {username},
                new UserIdMapper());

        // should be only one
        int userId = userIds.get(0);

        List<SerializableFormSession> sessions = new ArrayList<>();

        List<EntrySession> entrySessions = this.jdbcTemplate.query(
                replaceTableName("SELECT * FROM %s WHERE user_id = ?", POSTGRES_MIGRATED_ENTRYSESSION_TABLE_NAME),
                new Object[] {userId},
                new EntrySessionMapper());

        for (EntrySession entrySession: entrySessions) {
            String sessionId = entrySession.getSessionId();
            List<InstanceSession> instanceSessions = this.jdbcTemplate.query(
                    replaceTableName("SELECT * FROM %s WHERE sess_id = ?", POSTGRES_MIGRATED_SESSION_TABLE_NAME),
                    new Object[] {sessionId},
                    new InstanceSessionMapper());
            if(instanceSessions.size() < 1) {
                continue;
            }
            InstanceSession instanceSession = instanceSessions.get(0);
            SerializableFormSession session = loadSessionFromJson(instanceSession.getSessionJson());
            session.setDateOpened(entrySession.getCreatedDate());
            session.setId(entrySession.getSessionId());
            sessions.add(session);
        }
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
        return null;
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
        List<EntrySession> entrySessions = this.jdbcTemplate.query(
                replaceTableName("SELECT * FROM %s WHERE session_id = ?", POSTGRES_MIGRATED_ENTRYSESSION_TABLE_NAME),
                new Object[] {id},
                new EntrySessionMapper());

        List<InstanceSession> instanceSessions = this.jdbcTemplate.query(
                replaceTableName("SELECT * FROM %s WHERE sess_id = ?", POSTGRES_MIGRATED_SESSION_TABLE_NAME),
                new Object[] {id},
                new InstanceSessionMapper());

        InstanceSession instanceSession = instanceSessions.get(0);
        EntrySession entrySession = entrySessions.get(0);
        SerializableFormSession session = loadSessionFromJson(instanceSession.getSessionJson());
        session.setDateOpened(entrySession.getCreatedDate());
        session.setId(entrySession.getSessionId());
        return session;
    }

    @Override
    public boolean exists(String s) {
        String sql = replaceTableName("select exists(select 1 from %s where session_id = ?)",
                POSTGRES_MIGRATED_ENTRYSESSION_TABLE_NAME);
        return this.jdbcTemplate.queryForObject(sql, boolean.class, s);
    }

    @Override
    public Iterable<SerializableFormSession> findAll() {
        return null;
    }

    @Override
    public Iterable<SerializableFormSession> findAll(Iterable<String> strings) {
        return null;
    }

    @Override
    public long count() {
        return this.jdbcTemplate.queryForObject(
                replaceTableName("select count(*) from %s", POSTGRES_MIGRATED_ENTRYSESSION_TABLE_NAME), Integer.class);
    }

    @Override
    @Lock(LockModeType.OPTIMISTIC)
    public void delete(String id) {
        this.jdbcTemplate.update(replaceTableName("DELETE FROM %s WHERE session_id = ?",
                POSTGRES_MIGRATED_ENTRYSESSION_TABLE_NAME), id);
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

    public String replaceTableName(String query, String tableName){
        return String.format(query, tableName);
    }

    private class UserIdMapper implements RowMapper<Integer> {
        @Override
        public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getInt("id");
        }
    }

    private class InstanceSessionMapper implements RowMapper<InstanceSession> {

        @Override
        public InstanceSession mapRow(ResultSet rs, int rowNum) throws SQLException {
            InstanceSession instanceSession = new InstanceSession();
            instanceSession.setSessionId(rs.getString("sess_id"));
            instanceSession.setSessionJson(rs.getString("sess_json"));
            instanceSession.setLastModified(rs.getString("last_modified"));
            instanceSession.setCreatedDate(rs.getString("date_created"));
            return instanceSession;
        }
    }

    private class EntrySessionMapper implements RowMapper<EntrySession> {
        @Override
        public EntrySession mapRow(ResultSet rs, int rowNum) throws SQLException {
            EntrySession entrySession = new EntrySession();
            entrySession.setSessionId(rs.getString("session_id"));
            entrySession.setForm(rs.getString("form"));
            entrySession.setAppId(rs.getString("app_id"));
            entrySession.setSessionName(rs.getString("session_name"));
            entrySession.setCreatedDate(rs.getString("created_date"));
            entrySession.setLastModified(rs.getString("last_activity_date"));
            entrySession.setUserId(rs.getString("user_id"));
            return entrySession;
        }
    }

    private class InstanceSession {
        private String sessionId;
        private String sessionJson;
        private String lastModified;
        private String createdDate;

        public String getCreatedDate() {
            return createdDate;
        }

        public void setCreatedDate(String createdDate) {
            this.createdDate = createdDate;
        }

        public String getLastModified() {
            return lastModified;
        }

        public void setLastModified(String lastModified) {
            this.lastModified = lastModified;
        }

        public String getSessionJson() {
            return sessionJson;
        }

        public void setSessionJson(String sessionJson) {
            this.sessionJson = sessionJson;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
    }

    private class EntrySession {
        private String sessionId;
        private String form;
        private String appId;
        private String sessionName;
        private String createdDate;
        private String lastModified;
        private String userId;

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getForm() {
            return form;
        }

        public void setForm(String form) {
            this.form = form;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getSessionName() {
            return sessionName;
        }

        public void setSessionName(String sessionName) {
            this.sessionName = sessionName;
        }

        public String getCreatedDate() {
            return createdDate;
        }

        public void setCreatedDate(String createdDate) {
            this.createdDate = createdDate;
        }

        public String getLastModified() {
            return lastModified;
        }

        public void setLastModified(String lastModified) {
            this.lastModified = lastModified;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }


    public static SerializableFormSession loadSessionFromJson(String sessionJSON) {
        SerializableFormSession session = new SerializableFormSession();
        JSONObject sessionObject = new JSONObject(sessionJSON);
        JSONObject sessionData = sessionObject.getJSONObject("session_data");
        session.setTitle(sessionData.getString("session_name"));
        session.setUsername(sessionData.getString("username"));
        session.setDomain(sessionData.getString("domain"));
        session.setOneQuestionPerScreen(false);
        session.setAsUser(null);
        session.setInstanceXml(sessionObject.getString("instance"));
        session.setFormXml(sessionObject.getString("xform"));
        session.setInitLang(sessionObject.getString("init_lang"));
        session.setSequenceId(sessionObject.getInt("seq_id"));
        HashMap<String, String> sessionDataMap = new HashMap<>();
        session.setSessionData(sessionDataMap);
        if (sessionData.has("case_id")) {
            sessionDataMap.put("case_id", (String) sessionData.get("case_id"));
        }
        return session;
    }
}
