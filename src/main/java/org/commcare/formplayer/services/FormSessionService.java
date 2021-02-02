package org.commcare.formplayer.services;

import com.timgroup.statsd.StatsDClient;
import io.sentry.event.Event;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.exceptions.FormNotFoundException;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.repo.FormSessionRepo;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerSentry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FormSessionService {

    private final Log log = LogFactory.getLog(FormSessionService.class);

    @Autowired
    private FormSessionRepo formSessionRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StatsDClient datadogStatsDClient;

    @Autowired
    private FormplayerSentry raven;

    public int purge() {
        // Modeled on https://stackoverflow.com/a/6730401/2820312
        int deletedRows = 0;
        try {
            String createFuncQuery = "create or replace function custom_safe_cast(text,anyelement) \n" +
                    "returns anyelement \n" +
                    "language plpgsql as $$ \n" +
                    "begin \n" +
                    "    $0 := $1; \n" +
                    "    return $0; \n" +
                    "    exception when others then \n" +
                    "        return $2; \n" +
                    "end; $$;";
            this.jdbcTemplate.execute(createFuncQuery);
            String deleteQuery = String.format(
                    "delete from %s where custom_safe_cast(dateopened, '2011-01-01'::timestamp) < NOW() - INTERVAL '7 days';",
                    Constants.POSTGRES_SESSION_TABLE_NAME
            );
            log.info("Beginning state form session purge");
            long start = System.currentTimeMillis();
            deletedRows = this.jdbcTemplate.update(deleteQuery);
            long elapsed = System.currentTimeMillis() - start;
            log.info(String.format("Purged %d stale form sessions in %d ms", deletedRows, elapsed));
            datadogStatsDClient.time("PostgresFormSessionRepo.purge.timeInMillis", elapsed);
        } catch (Exception e) {
            log.error("Exception purge form sessions", e);
            raven.sendRavenException(e, Event.Level.ERROR);
        }
        return deletedRows;
    }

    public SerializableFormSession getSessionById(String id) {
        Optional<SerializableFormSession> session = formSessionRepo.findById(id);
        if (!session.isPresent()) {
            throw new FormNotFoundException(id);
        }
        return session.get();
    }

    public List<SerializableFormSession> getSessionsForUser(String username) {
        return formSessionRepo.findUserSessions(username);
    }

    public SerializableFormSession saveSession(SerializableFormSession session) {
        return formSessionRepo.save(session);
    }

    public void deleteSessionById(String id) {
        formSessionRepo.deleteById(id);
    }
}
