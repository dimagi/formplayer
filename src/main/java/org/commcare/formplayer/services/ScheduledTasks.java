package org.commcare.formplayer.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.Application;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

import net.javacrumbs.shedlock.core.SchedulerLock;

@Component
public class ScheduledTasks {

    private final Log log = LogFactory.getLog(ScheduledTasks.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Scheduled(fixedRate = 5000)
    @SchedulerLock(name = "reportCurrentTime", lockAtMostFor = 14000, lockAtLeastFor = 14000)
    public void reportCurrentTime() {
        log.info("The time is now {}".format(dateFormat.format(new Date())));
    }
}
