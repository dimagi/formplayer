package org.commcare.formplayer.services;

import com.timgroup.statsd.StatsDClient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
public class ScheduledTasks {

    private final Log log = LogFactory.getLog(ScheduledTasks.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Autowired
    private StatsDClient datadogStatsDClient;

    @Autowired
    private FormSessionService formSessionService;

    // the default "0 0 0 * * *" schedule means midnight each night
    @Scheduled(cron = "${commcare.formplayer.scheduledTasks.purge.cron:0 0 0 * * *}")
    @SchedulerLock(name = "purge",
            lockAtMostFor = "${commcare.formplayer.scheduledTasks.purge.lockAtMostFor:5h}",
            lockAtLeastFor = "${commcare.formplayer.scheduledTasks.purge.lockAtLeastFor:1h}")
    public void purge() {
        log.info("Starting purge scheduled task.");
        int deletedRows = formSessionService.purge();
        datadogStatsDClient.count(
                String.format("%s.%s", Constants.SCHEDULED_TASKS_PURGE, "deletedRows"),
                deletedRows
        );
        datadogStatsDClient.increment(
                String.format("%s.%s", Constants.SCHEDULED_TASKS_PURGE, "timesRun")
        );
    }
}
