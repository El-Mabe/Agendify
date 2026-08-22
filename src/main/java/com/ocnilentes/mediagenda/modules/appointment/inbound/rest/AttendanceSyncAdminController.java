package com.ocnilentes.mediagenda.modules.appointment.inbound.rest;

import com.ocnilentes.mediagenda.modules.appointment.application.AttendanceSyncService;
import com.ocnilentes.mediagenda.shared.config.AttendanceSyncProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/attendance")
public class AttendanceSyncAdminController {

    private final AttendanceSyncService attendanceSyncService;
    private final AttendanceSyncProperties properties;

    public AttendanceSyncAdminController(
            AttendanceSyncService attendanceSyncService,
            AttendanceSyncProperties properties
    ) {
        this.attendanceSyncService = attendanceSyncService;
        this.properties = properties;
    }

    @PostMapping("/sync")
    public ResponseEntity<?> sync(
            @RequestHeader(value = "X-Attendance-Token", required = false) String token,
            @RequestParam(defaultValue = "false") boolean force
    ) {
        if (StringUtils.hasText(properties.manualRunToken())
                && !properties.manualRunToken().equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid attendance token");
        }
        return ResponseEntity.ok(attendanceSyncService.syncAttendedAppointments(force));
    }
}
