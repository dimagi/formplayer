package org.commcare.formplayer.services;

import com.timgroup.statsd.StatsDClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.beans.FormsSessionsRequestBean;
import org.commcare.formplayer.exceptions.FormNotFoundException;
import org.commcare.formplayer.objects.FormSessionListView;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.repo.FormSessionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@CacheConfig(cacheNames = {"form_session"})
public class FormSessionService {

    private final Log log = LogFactory.getLog(FormSessionService.class);

    private static int DEFAULT_FORMS_PER_PAGE = 10;
    private static int MAX_FORMS_PER_PAGE = 100;

    @Autowired
    private FormSessionRepo formSessionRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    CacheManager cacheManager;

    @Autowired(required = false)
    private StatsDClient datadogStatsDClient;

    @CacheEvict(allEntries = true)
    public int purge() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        log.info("Beginning state form session purge");
        long start = System.currentTimeMillis();
        int deletedRows = formSessionRepo.deleteSessionsOlderThan(cutoff);
        long elapsed = System.currentTimeMillis() - start;
        log.info(String.format("Purged %d stale form sessions in %d ms", deletedRows, elapsed));
        if (datadogStatsDClient != null) {
            datadogStatsDClient.time("PostgresFormSessionRepo.purge.timeInMillis", elapsed);
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

    public List<FormSessionListView> getSessionsForUser(String username, FormsSessionsRequestBean formsSessionsRequest) {
        String domain = formsSessionsRequest.getDomain();
        String asUser = formsSessionsRequest.getRestoreAs();
        int pageSize = getPageSize(formsSessionsRequest);

        int pageNumber = formsSessionsRequest.getPageNumber();
        Pageable page = PageRequest.of(pageNumber, pageSize);

        if (asUser == null) {
             return formSessionRepo.findByUsernameAndDomainAndAsUserIsNullOrderByDateCreatedDesc(username, domain, page);
        } else {
             return formSessionRepo.findByUsernameAndDomainAndAsUserOrderByDateCreatedDesc(username, domain, asUser, page);
        }
    }

    public long getNumberOfSessionsForUser(String username, FormsSessionsRequestBean formsSessionsRequest) {
        String domain = formsSessionsRequest.getDomain();
        String asUser = formsSessionsRequest.getRestoreAs();

        if (asUser == null) {
            return formSessionRepo.countByUsernameAndDomainAndAsUserIsNullOrderByDateCreatedDesc(username, domain);
        } else {
            return formSessionRepo.countByUsernameAndDomainAndAsUserOrderByDateCreatedDesc(username, domain, asUser);
        }
    }

    private int getPageSize(FormsSessionsRequestBean formsSessionsRequest) {
        int pageSize = formsSessionsRequest.getPageSize();
        if (pageSize == 0) {
            pageSize = DEFAULT_FORMS_PER_PAGE;
        }
        pageSize = Math.min(pageSize, MAX_FORMS_PER_PAGE);
        return pageSize;
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
