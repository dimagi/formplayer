package org.commcare.formplayer.services;

import com.timgroup.statsd.StatsDClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.Application;
import org.commcare.formplayer.repo.FormSessionRepo;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
public class ScheduledTasks {

    private final Log log = LogFactory.getLog(ScheduledTasks.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Autowired
    protected FormSessionRepo formSessionRepo;

    @Autowired
    private StatsDClient datadogStatsDClient;

    // the default "-" corresponds to Scheduled.CRON_DISABLED
    @Scheduled(cron= "${commcare.formplayer.scheduledTasks.purge.cron:-}")
    @SchedulerLock(name = "purge",
            lockAtMostFor = "${commcare.formplayer.scheduledTasks.purge.lockAtMostFor:5h}",
            lockAtLeastFor = "${commcare.formplayer.scheduledTasks.purge.lockAtLeastFor:1h}")
    public void purge() {
        log.info("Starting purge scheduled task.");
        int deletedRows = formSessionRepo.purge();
        datadogStatsDClient.count(
                "%s.%s".format(Constants.SCHEDULED_TASKS_PURGE, "deletedRows"),
                deletedRows
        );
        datadogStatsDClient.increment(
                "%s.%s".format(Constants.SCHEDULED_TASKS_PURGE, "timesRun")
        );
    }
}
