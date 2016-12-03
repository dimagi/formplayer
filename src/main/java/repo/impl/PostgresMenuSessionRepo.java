package repo.impl;

import exceptions.MenuNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import repo.MenuSessionRepo;
import repo.SerializableMenuSession;
import util.Constants;

import javax.persistence.LockModeType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * Postgres implementation for storing form entry sessions
 * Corresponds to the new_formplayer_session table in the formplayer database
 */
@Repository
public class PostgresMenuSessionRepo implements MenuSessionRepo {

    @Autowired
    @Qualifier("formplayerTemplate")
    private JdbcTemplate jdbcTemplate;

    @Override
    @Lock(LockModeType.OPTIMISTIC)
    public <S extends SerializableMenuSession> Iterable<S> save(Iterable<S> entities) {
        for(SerializableMenuSession session: entities){
            save(session);
        }
        return entities;
    }

    @Override
    @Lock(LockModeType.OPTIMISTIC)
    public <S extends SerializableMenuSession> S save(S session) {

        int sessionCount = this.jdbcTemplate.queryForObject(
                replaceTableName("select count(*) from %s where id = ?"), Integer.class, session.getId());


        if(sessionCount > 0){
            String query = replaceTableName("UPDATE %s SET commcaresession = ? WHERE id = ?");
            this.jdbcTemplate.update(query,  new Object[] {session.getCommcareSession(), session.getId()},
                    new int[] {Types.BINARY, Types.VARCHAR});
            return session;

        }

        String query = replaceTableName("INSERT into %s " +
                "(id, username, domain, appid, " +
                "installreference, locale, commcaresession, asUser) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        this.jdbcTemplate.update(query,  new Object[] {session.getId(), session.getUsername(), session.getDomain(),
                session.getAppId(), session.getInstallReference(), session.getLocale(), session.getCommcareSession(),
        session.getAsUser()}, new int[] {
                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                Types.VARCHAR, Types.VARCHAR, Types.BINARY, Types.VARCHAR});
        return session;
    }

    @Override
    public SerializableMenuSession findOne(String id) {
        String sql = replaceTableName("SELECT * FROM %s WHERE id = ?");
        return jdbcTemplate.queryForObject(sql, new Object[] {id}, new SessionMapper());
    }

    @Override
    public boolean exists(String s) {
        String sql = replaceTableName("select exists(select 1 from %s where id = ?)");
        return this.jdbcTemplate.queryForObject(sql, boolean.class, s);
    }

    @Override
    public Iterable<SerializableMenuSession> findAll() {
        List<SerializableMenuSession> sessions = this.jdbcTemplate.query(
                replaceTableName("SELECT * FROM %s"),
                new SessionMapper());
        return sessions;
    }

    @Override
    public Iterable<SerializableMenuSession> findAll(Iterable<String> strings) {
        List<SerializableMenuSession> sessions = this.jdbcTemplate.query(
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
    public void delete(SerializableMenuSession entity) {
        delete(entity.getId());
    }

    @Override
    public void delete(Iterable<? extends SerializableMenuSession> entities) {
        for(SerializableMenuSession session: entities){
            delete(session.getId());
        }
    }

    @Override
    public void deleteAll() {
        Iterable<SerializableMenuSession> allSessions = findAll();
        for(SerializableMenuSession session: allSessions){
            delete(session.getId());
        }
    }

    @Override
    public SerializableMenuSession findOneWrapped(String id) throws MenuNotFoundException {
        try {
            return findOne(id);
        } catch(EmptyResultDataAccessException e) {
            throw new MenuNotFoundException(id);
        }
    }

    // TODO WSP Write migration for this to have OQPS boolean after asUser is merger
    // helper class for mapping a db row to a serialized session
    private static final class SessionMapper implements RowMapper<SerializableMenuSession> {

        public SerializableMenuSession mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SerializableMenuSession(rs.getString("id"),
                    rs.getString("username"),
                    rs.getString("domain"),
                    rs.getString("appid"),
                    rs.getString("installreference"),
                    rs.getString("locale"),
                    (byte[]) rs.getObject("commcaresession"),
                    false,
                    rs.getString("asUser"));
        }
    }

    public String replaceTableName(String query){
        return String.format(query, Constants.POSTGRES_MENU_SESSION_TABLE_NAME);
    }

}
