package com.ocnilentes.mediagenda.modules.appointment.application;

import com.ocnilentes.mediagenda.modules.appointment.outbound.sheets.AppointmentSheetImportService;
import com.ocnilentes.mediagenda.shared.config.AppointmentSheetSyncProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AppointmentSheetReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentSheetReconciliationService.class);

    private final AppointmentSheetSyncProperties properties;
    private final ObjectProvider<AppointmentSheetImportService> importServiceProvider;

    public AppointmentSheetReconciliationService(
            AppointmentSheetSyncProperties properties,
            ObjectProvider<AppointmentSheetImportService> importServiceProvider
    ) {
        this.properties = properties;
        this.importServiceProvider = importServiceProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {
        if (!properties.enabled() || !properties.bootstrapEnabled()) {
            return;
        }
        runImport(
                properties.bootstrapMonthsBack(),
                properties.bootstrapMonthsForward(),
                true
        );
    }

    @Scheduled(fixedDelayString = "${appointments.sheet-sync.reconciliation-delay-ms:600000}")
    public void reconcile() {
        if (!properties.enabled() || !properties.reconciliationEnabled()) {
            return;
        }
        // La reconciliación recurrente solo incorpora cambios; nunca cancela
        // PostgreSQL por ausencia de una fila en la proyección de Sheets.
        runImport(0, properties.reconciliationMonthsForward(), false);
    }

    public synchronized AppointmentSheetImportService.ImportResult runImport(
            int monthsBack,
            int monthsForward,
            boolean reconcileDeletes
    ) {
        AppointmentSheetImportService importService = importServiceProvider.getIfAvailable();
        if (importService == null) {
            return new AppointmentSheetImportService.ImportResult(0, 0, 0, 0, 1);
        }
        AppointmentSheetImportService.ImportResult result =
                importService.importRange(monthsBack, monthsForward, reconcileDeletes);
        log.info(
                "Appointment Sheets import finished created={} updated={} unchanged={} cancelled={} failed={}",
                result.created(),
                result.updated(),
                result.unchanged(),
                result.cancelled(),
                result.failed()
        );
        return result;
    }
}
