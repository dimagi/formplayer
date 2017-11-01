package repo.impl;

import exceptions.FormNotFoundException;
import objects.SerializableFormSession;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javarosa.core.model.FormDef;
import org.javarosa.xform.parse.XFormParser;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import repo.FormSessionRepo;
import services.RestoreFactory;
import util.FormplayerDateUtils;
import util.StringUtils;

import javax.persistence.LockModeType;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Repository for reading Old CloudCare incomplete form sessions into the new format
 * Repo has read only access
 * In addition to reading from the database, this repository handles the migration including:
 *  - Parsing the old JSON blob into our POJO
 *  - Adding the PostURL since the old one didn't contain it
 *  - Converting the formXml into a serialized version
 */
@Repository
public class PostgresMigratedFormSessionRepo implements FormSessionRepo {

    @Autowired
    @Qualifier("hqTemplate")
    private JdbcTemplate jdbcTemplate;

    @Value("${commcarehq.host}")
    private String host;

    @Autowired
    private RestoreFactory restoreFactory;

    private static final Log log = LogFactory.getLog(PostgresMigratedFormSessionRepo.class);

    public static final String POSTGRES_MIGRATED_SESSION_TABLE_NAME = "formplayer_session";
    public static final String POSTGRES_MIGRATED_ENTRYSESSION_TABLE_NAME = "formplayer_entrysession";
    public static final String POSTGRES_AUTH_USER_TABLE_NAME = "auth_user";

    @Override
    public List<SerializableFormSession> findUserSessions(String username) {

        List<Integer> userIds = this.jdbcTemplate.query(
                String.format("SELECT * FROM %s WHERE username = ?", POSTGRES_AUTH_USER_TABLE_NAME),
                new Object[] {username},
                new UserIdMapper());

        // should be only one
        int userId = userIds.get(0);

        List<SerializableFormSession> sessions = new ArrayList<>();

        List<EntrySession> entrySessions = this.jdbcTemplate.query(
                String.format("SELECT * FROM %s WHERE user_id = ?", POSTGRES_MIGRATED_ENTRYSESSION_TABLE_NAME),
                new Object[] {userId},
                new EntrySessionMapper());

        for (EntrySession entrySession: entrySessions) {
            String sessionId = entrySession.getSessionId();
            List<InstanceSession> instanceSessions = this.jdbcTemplate.query(
                    String.format("SELECT * FROM %s WHERE sess_id = ?", POSTGRES_MIGRATED_SESSION_TABLE_NAME),
                    new Object[] {sessionId},
                    new InstanceSessionMapper());
            if(instanceSessions.size() < 1) {
                continue;
            }
            sessions.add(buildSerializedSession(entrySession, instanceSessions.get(0)));
        }
        return sessions;
    }

    @Override
    @Lock(LockModeType.OPTIMISTIC)
    public <S extends SerializableFormSession> Iterable<S> save(Iterable<S> entities) {
        throw new RuntimeException("Tried to save " + entities + " into the Old CloudCare session DB. " +
                "We should not be updating these entries");
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
        throw new RuntimeException("Tried to save " + session + " into the Old CloudCare session DB. " +
                "We should not be updating these entries");
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
                String.format("SELECT * FROM %s WHERE session_id = ?", POSTGRES_MIGRATED_ENTRYSESSION_TABLE_NAME),
                new Object[] {id},
                new EntrySessionMapper());

        List<InstanceSession> instanceSessions = this.jdbcTemplate.query(
                String.format("SELECT * FROM %s WHERE sess_id = ?", POSTGRES_MIGRATED_SESSION_TABLE_NAME),
                new Object[] {id},
                new InstanceSessionMapper());

        InstanceSession instanceSession = instanceSessions.get(0);
        EntrySession entrySession = entrySessions.get(0);
        return buildSerializedSession(entrySession, instanceSession);
    }

    public SerializableFormSession buildSerializedSession(EntrySession entrySession, InstanceSession instanceSession) {
        SerializableFormSession session = loadSessionFromJson(instanceSession.getSessionJson());
        session.setDateOpened(FormplayerDateUtils.convertIsoToJavaDate(entrySession.getCreatedDate()));
        session.setId(entrySession.getSessionId());

        if(session.getPostUrl() == null) {
            session.setPostUrl(
                    StringUtils.getPostUrl(host,
                            session.getDomain(),
                            entrySession.getAppId()));
        }
        FormDef formDef = parseFormDef(session.getFormXml());
        session.setFormXml(serializeFormDef(formDef));

        return session;
    }

    @Override
    public boolean exists(String s) {
        String sql = String.format("select exists(select 1 from %s where session_id = ?)",
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
                String.format("select count(*) from %s", POSTGRES_MIGRATED_ENTRYSESSION_TABLE_NAME), Integer.class);
    }

    @Override
    @Lock(LockModeType.OPTIMISTIC)
    public void delete(String id) {
        this.jdbcTemplate.update(String.format("DELETE FROM %s WHERE session_id = ?",
                POSTGRES_MIGRATED_ENTRYSESSION_TABLE_NAME), id);
        this.jdbcTemplate.update(String.format("DELETE FROM %s WHERE sess_id = ?",
                POSTGRES_MIGRATED_SESSION_TABLE_NAME), id);
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
        // default behavior of null locale just results in default
        if (sessionObject.has("init_lang")) {
            session.setInitLang(sessionObject.getString("init_lang"));
        }
        session.setSequenceId(sessionObject.getInt("seq_id"));
        HashMap<String, String> sessionDataMap = new HashMap<>();
        session.setSessionData(sessionDataMap);
        if (sessionData.has("case_id")) {
            sessionDataMap.put("case_id", (String) sessionData.get("case_id"));
        }
        return session;
    }

    // Copied from FormSession
    private static String serializeFormDef(FormDef formDef) {
        try {
            org.apache.commons.io.output.ByteArrayOutputStream baos = new org.apache.commons.io.output.ByteArrayOutputStream();
            DataOutputStream serializedStream = new DataOutputStream(baos);
            formDef.writeExternal(serializedStream);
            return Base64.encodeBase64String(baos.toByteArray());
        } catch(Exception e) {
            log.error("Got exception " + e + " parsing formDef " + formDef);
            return null;
        }
    }
    // Copied from NewFormResponseFactory
    private static FormDef parseFormDef(String formXml) {
        try {
            XFormParser mParser = new XFormParser(new StringReader(formXml));
            return mParser.parse();
        } catch(Exception e) {
            log.error("Got exception " + e + " parsing formXml " + formXml);
            return null;
        }
    }
}
