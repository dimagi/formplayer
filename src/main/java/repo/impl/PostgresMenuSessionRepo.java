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
import java.util.Optional;

/**
 * Postgres implementation for storing form entry sessions
 * Corresponds to the new_formplayer_session table in the formplayer database
 */
@Repository
public class PostgresMenuSessionRepo implements MenuSessionRepo {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @Lock(LockModeType.OPTIMISTIC)
    public <S extends SerializableMenuSession> Iterable<S> saveAll(Iterable<S> entities) {
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
                "installreference, locale, commcaresession, asUser, preview) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
        this.jdbcTemplate.update(query,  new Object[] {session.getId(), session.getUsername(), session.getDomain(),
                session.getAppId(), session.getInstallReference(), session.getLocale(), session.getCommcareSession(),
        session.getAsUser(), session.getPreview()}, new int[] {
                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                Types.VARCHAR, Types.VARCHAR, Types.BINARY, Types.VARCHAR, Types.BOOLEAN});
        return session;
    }

    @Override
    public Optional<SerializableMenuSession> findById(String id) throws MenuNotFoundException {
        String sql = replaceTableName("SELECT * FROM %s WHERE id = ?");
        try {
            return Optional.of(jdbcTemplate.queryForObject(sql, new Object[]{id}, new SessionMapper()));
        } catch (EmptyResultDataAccessException e) {
            throw new MenuNotFoundException(id);
        }
    }

    @Override
    public boolean existsById(String s) {
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
    public Iterable<SerializableMenuSession> findAllById(Iterable<String> strings) {
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
    public void deleteById(String id) {
        this.jdbcTemplate.update(replaceTableName("DELETE FROM %s WHERE id = ?"), id);
    }

    @Override
    public void delete(SerializableMenuSession entity) {
        deleteById(entity.getId());
    }

    @Override
    public void deleteAll(Iterable<? extends SerializableMenuSession> entities) {
        for(SerializableMenuSession session: entities){
            deleteById(session.getId());
        }
    }

    @Override
    public void deleteAll() {
        Iterable<SerializableMenuSession> allSessions = findAll();
        for(SerializableMenuSession session: allSessions){
            deleteById(session.getId());
        }
    }

    @Override
    public SerializableMenuSession findOneWrapped(String id) throws MenuNotFoundException {
        return findById(id).get();
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
                    rs.getString("asUser"),
                    rs.getBoolean("preview"));
        }
    }

    public String replaceTableName(String query){
        return String.format(query, Constants.POSTGRES_MENU_SESSION_TABLE_NAME);
    }

}
