package com.ocnilentes.mediagenda.modules.reminder.inbound.rest;

import com.ocnilentes.mediagenda.modules.reminder.application.ReminderDispatchService;
import com.ocnilentes.mediagenda.shared.config.ReminderProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/reminders")
public class ReminderAdminController {

    private final ReminderDispatchService reminderDispatchService;
    private final ReminderProperties reminderProperties;

    public ReminderAdminController(
            ReminderDispatchService reminderDispatchService,
            ReminderProperties reminderProperties
    ) {
        this.reminderDispatchService = reminderDispatchService;
        this.reminderProperties = reminderProperties;
    }

    @PostMapping("/run")
    public ResponseEntity<?> run(
            @RequestHeader(value = "X-Reminder-Token", required = false) String token,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean force
    ) {
        if (StringUtils.hasText(reminderProperties.manualRunToken())
                && !reminderProperties.manualRunToken().equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid reminder token");
        }

        return ResponseEntity.ok(reminderDispatchService.dispatchDueReminders(force));
    }
}
