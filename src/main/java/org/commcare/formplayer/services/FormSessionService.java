package org.commcare.formplayer.services;

import com.timgroup.statsd.StatsDClient;
import io.sentry.event.Event;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.exceptions.FormNotFoundException;
import org.commcare.formplayer.objects.FormSessionListView;
import org.commcare.formplayer.objects.FormSessionListViewRaw;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.repo.FormSessionRepo;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerSentry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = {"form_session"})
public class FormSessionService {

    private final Log log = LogFactory.getLog(FormSessionService.class);

    @Autowired
    private FormSessionRepo formSessionRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    CacheManager cacheManager;

    @Autowired(required = false)
    private StatsDClient datadogStatsDClient;

    @Autowired(required = false)
    private FormplayerSentry raven;

    @CacheEvict(allEntries = true)
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
            if (datadogStatsDClient != null) {
                datadogStatsDClient.time("PostgresFormSessionRepo.purge.timeInMillis", elapsed);
            }
        } catch (Exception e) {
            log.error("Exception purge form sessions", e);
            if (raven != null) {
                raven.sendRavenException(e, Event.Level.ERROR);
            }
        }
        return deletedRows;
    }

    @Cacheable
    public SerializableFormSession getSessionById(String id) {
        Optional<SerializableFormSession> session = formSessionRepo.findById(id);
        if (!session.isPresent()) {
            throw new FormNotFoundException(id);
        }
        return session.get();
    }

    public List<FormSessionListView> getSessionsForUser(String username) {
        // Replace blow code with this line once we can remove custom ordering on ``dateOpened``
        // return formSessionRepo.findByUsername(username, Sort.by(Sort.Direction.DESC, "dateCreated"));

        List<FormSessionListViewRaw> userSessionsRaw = formSessionRepo.findUserSessions(username);
        return userSessionsRaw.stream().map((session) -> new FormSessionListView() {
            @Override
            public String getId() {
                return session.getId();
            }

            @Override
            public String getTitle() {
                return session.getTitle();
            }

            @Override
            public String getDateOpened() {
                return session.getDateOpened();
            }

            @Override
            public Instant getDateCreated() {
                return session.getDateCreated();
            }

            @Override
            public Map<String, String> getSessionData() {
                return (Map<String, String>) SerializationUtils.deserialize(session.getSessionData());
            }
        }).collect(Collectors.toList());
    }

    @CachePut(key = "#session.id")
    public SerializableFormSession saveSession(SerializableFormSession session) {
        return formSessionRepo.save(session);
    }

    @CacheEvict
    public void deleteSessionById(String id) {
        formSessionRepo.deleteById(id);
    }
}
