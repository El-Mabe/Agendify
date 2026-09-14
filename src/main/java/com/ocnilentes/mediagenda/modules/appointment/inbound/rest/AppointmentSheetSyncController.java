package com.ocnilentes.mediagenda.modules.appointment.inbound.rest;

import com.ocnilentes.mediagenda.modules.appointment.application.AppointmentSheetReconciliationService;
import com.ocnilentes.mediagenda.modules.appointment.application.ReceptionSheetSyncService;
import com.ocnilentes.mediagenda.modules.appointment.outbound.sheets.AppointmentSheetImportService;
import com.ocnilentes.mediagenda.shared.config.AppointmentSheetSyncProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/appointments/sheets")
public class AppointmentSheetSyncController {

    private static final String TOKEN_HEADER = "X-Sheet-Sync-Token";

    private final ReceptionSheetSyncService syncService;
    private final AppointmentSheetReconciliationService reconciliationService;
    private final AppointmentSheetSyncProperties properties;

    public AppointmentSheetSyncController(
            ReceptionSheetSyncService syncService,
            AppointmentSheetReconciliationService reconciliationService,
            AppointmentSheetSyncProperties properties
    ) {
        this.syncService = syncService;
        this.reconciliationService = reconciliationService;
        this.properties = properties;
    }

    @PostMapping("/row")
    public ResponseEntity<ReceptionSheetSyncService.SyncResult> synchronizeRow(
            @RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @RequestBody ReceptionSheetSyncService.SheetRowChange change
    ) {
        requireValidToken(token);
        return ResponseEntity.ok(syncService.apply(change));
    }

    @PostMapping("/import")
    public ResponseEntity<AppointmentSheetImportService.ImportResult> importSheets(
            @RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @RequestParam(defaultValue = "1") int monthsBack,
            @RequestParam(defaultValue = "3") int monthsForward,
            @RequestParam(defaultValue = "false") boolean reconcileDeletes
    ) {
        requireValidToken(token);
        if (monthsBack < 0 || monthsBack > 12 || monthsForward < 0 || monthsForward > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rango permitido es de 0 a 12 meses");
        }
        return ResponseEntity.ok(
                reconciliationService.runImport(monthsBack, monthsForward, reconcileDeletes)
        );
    }

    private void requireValidToken(String providedToken) {
        if (!properties.enabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!StringUtils.hasText(properties.webhookToken())) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Appointment Sheets sync token is not configured"
            );
        }
        byte[] expected = properties.webhookToken().getBytes(StandardCharsets.UTF_8);
        byte[] provided = providedToken == null
                ? new byte[0]
                : providedToken.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, provided)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
