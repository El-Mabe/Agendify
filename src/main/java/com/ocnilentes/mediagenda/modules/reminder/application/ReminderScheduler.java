package com.ocnilentes.mediagenda.modules.reminder.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderDispatchService reminderDispatchService;

    public ReminderScheduler(ReminderDispatchService reminderDispatchService) {
        this.reminderDispatchService = reminderDispatchService;
    }

    @Scheduled(cron = "${reminder.cron:0 */15 18-22 * * *}", zone = "America/Bogota")
    public void runScheduledDispatch() {
        ReminderDispatchService.ReminderRunResult result = reminderDispatchService.dispatchDueReminders();
        log.debug(
                "Scheduled reminder run enabled={} insideWindow={} sent={} failed={}",
                result.enabled(),
                result.insideWindow(),
                result.sent(),
                result.failed()
        );
    }
}
