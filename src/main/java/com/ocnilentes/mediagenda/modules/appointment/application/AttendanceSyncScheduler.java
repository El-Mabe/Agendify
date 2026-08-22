package com.ocnilentes.mediagenda.modules.appointment.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AttendanceSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(AttendanceSyncScheduler.class);

    private final AttendanceSyncService attendanceSyncService;

    public AttendanceSyncScheduler(AttendanceSyncService attendanceSyncService) {
        this.attendanceSyncService = attendanceSyncService;
    }

    @Scheduled(cron = "${attendance-sync.cron:0 */15 20-22 * * *}", zone = "America/Bogota")
    public void runScheduledSync() {
        AttendanceSyncService.AttendanceSyncResult result = attendanceSyncService.syncAttendedAppointments();
        log.debug(
                "Scheduled attendance sync enabled={} insideWindow={} processed={} failed={}",
                result.enabled(),
                result.insideWindow(),
                result.processed(),
                result.failed()
        );
    }
}
