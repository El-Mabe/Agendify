package com.ocnilentes.mediagenda.modules.appointment.outbound.sheets;

import com.ocnilentes.mediagenda.modules.appointment.domain.exception.AppointmentException;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.AppointmentJpaEntity;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.ClinicHolidayJpaEntity;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.JpaAppointmentRepository;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.SpringDataClinicHolidayRepository;
import com.ocnilentes.mediagenda.modules.information.outbound.persistence.ClinicInformationJpaEntity;
import com.ocnilentes.mediagenda.modules.information.outbound.persistence.SpringDataClinicInformationRepository;
import com.ocnilentes.mediagenda.shared.config.GoogleSheetsProperties;
import com.ocnilentes.mediagenda.shared.outbound.google.GoogleSheetsApiClient;
import com.ocnilentes.mediagenda.shared.outbound.google.GoogleSheetsException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "google.sheets", name = "enabled", havingValue = "true")
public class AppointmentSheetImportService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentSheetImportService.class);
    private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Bogota");

    private final GoogleSheetsApiClient sheetsApiClient;
    private final JpaAppointmentRepository appointmentRepository;
    private final SpringDataClinicHolidayRepository holidayRepository;
    private final SpringDataClinicInformationRepository informationRepository;
    private final GoogleSheetsProperties sheetsProperties;

    public AppointmentSheetImportService(
            GoogleSheetsApiClient sheetsApiClient,
            JpaAppointmentRepository appointmentRepository,
            SpringDataClinicHolidayRepository holidayRepository,
            SpringDataClinicInformationRepository informationRepository,
            GoogleSheetsProperties sheetsProperties
    ) {
        this.sheetsApiClient = sheetsApiClient;
        this.appointmentRepository = appointmentRepository;
        this.holidayRepository = holidayRepository;
        this.informationRepository = informationRepository;
        this.sheetsProperties = sheetsProperties;
    }

    public ImportResult importRange(int monthsBack, int monthsForward, boolean reconcileDeletes) {
        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int cancelled = 0;
        int failed = 0;
        YearMonth current = YearMonth.from(LocalDate.now(CLINIC_ZONE));
        if (!synchronizeHolidays()) {
            failed++;
        }
        if (!synchronizeInformation()) {
            failed++;
        }

        for (int offset = -Math.max(0, monthsBack); offset <= Math.max(0, monthsForward); offset++) {
            LocalDate probe = current.plusMonths(offset).atDay(1);
            for (Consultorio consultorio : Consultorio.values()) {
                String spreadsheetId = sheetsProperties.resolveConsultorioSpreadsheetId(consultorio);
                String sheetName = VisualAgendaSheetParser.sheetName(consultorio, probe);
                try {
                    SheetImportResult result = importSheet(
                            spreadsheetId,
                            sheetName,
                            consultorio,
                            reconcileDeletes
                    );
                    created += result.created();
                    updated += result.updated();
                    unchanged += result.unchanged();
                    cancelled += result.cancelled();
                    failed += result.failed();
                } catch (GoogleSheetsException exception) {
                    if (isMissingSheet(exception)) {
                        log.info("Skipping missing appointment sheet={}", sheetName);
                    } else {
                        failed++;
                        log.error("Failed to import appointment sheet={}", sheetName, exception);
                    }
                }
            }
        }
        return new ImportResult(created, updated, unchanged, cancelled, failed);
    }

    private boolean synchronizeHolidays() {
        String spreadsheetId = sheetsProperties.resolveSharedSpreadsheetId();
        String sheet = StringUtils.hasText(sheetsProperties.holidaysSheet())
                ? sheetsProperties.holidaysSheet()
                : "Festivos";
        try {
            List<ClinicHolidayJpaEntity> incoming = new ArrayList<>();
            Set<LocalDate> incomingDates = new HashSet<>();
            for (List<Object> row : sheetsApiClient.getValues(spreadsheetId, sheet + "!A2:A")) {
                if (row == null || row.isEmpty() || row.getFirst() == null) {
                    continue;
                }
                try {
                    LocalDate date = LocalDate.parse(
                            String.valueOf(row.getFirst()).trim(),
                            DateTimeFormatter.ISO_LOCAL_DATE
                    );
                    if (incomingDates.add(date)) {
                        incoming.add(new ClinicHolidayJpaEntity(date));
                    }
                } catch (RuntimeException ignored) {
                    // Omite filas que no contienen fechas ISO.
                }
            }
            holidayRepository.saveAll(incoming);
            List<ClinicHolidayJpaEntity> obsolete = holidayRepository.findAll().stream()
                    .filter(holiday -> !incomingDates.contains(holiday.getDate()))
                    .toList();
            holidayRepository.deleteAll(obsolete);
            return true;
        } catch (GoogleSheetsException exception) {
            log.error("Failed to synchronize clinic holidays", exception);
            return false;
        }
    }

    private boolean synchronizeInformation() {
        String spreadsheetId = sheetsProperties.resolveSharedSpreadsheetId();
        String sheet = StringUtils.hasText(sheetsProperties.informationSheet())
                ? sheetsProperties.informationSheet()
                : "Informacion";
        try {
            List<ClinicInformationJpaEntity> incoming = new ArrayList<>();
            Set<String> incomingKeys = new HashSet<>();
            for (List<Object> row : sheetsApiClient.getValues(spreadsheetId, sheet + "!A2:B")) {
                if (row == null || row.size() < 2 || row.get(0) == null || row.get(1) == null) {
                    continue;
                }
                String key = String.valueOf(row.get(0)).trim().toLowerCase();
                String value = String.valueOf(row.get(1)).trim();
                if (StringUtils.hasText(key) && StringUtils.hasText(value) && incomingKeys.add(key)) {
                    incoming.add(new ClinicInformationJpaEntity(key, value));
                }
            }
            informationRepository.saveAll(incoming);
            List<ClinicInformationJpaEntity> obsolete = informationRepository.findAll().stream()
                    .filter(entry -> !incomingKeys.contains(entry.getKey()))
                    .toList();
            informationRepository.deleteAll(obsolete);
            return true;
        } catch (GoogleSheetsException exception) {
            if (isMissingSheet(exception)) {
                log.info("Skipping optional clinic information sheet={}", sheet);
                return true;
            }
            log.error("Failed to synchronize clinic information", exception);
            return false;
        }
    }

    private SheetImportResult importSheet(
            String spreadsheetId,
            String sheetName,
            Consultorio consultorio,
            boolean reconcileDeletes
    ) {
        List<List<Object>> rows = sheetsApiClient.getValues(spreadsheetId, sheetName + "!A1:G");
        List<Appointment> appointments = VisualAgendaSheetParser.findAppointmentsInSheet(rows, consultorio);
        Set<Integer> occupiedRows = new HashSet<>();
        Map<String, List<List<Object>>> identityWrites = new LinkedHashMap<>();
        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int failed = 0;

        for (Appointment appointment : appointments) {
            if (appointment.sheetRowNumber() == null) {
                continue;
            }
            occupiedRows.add(appointment.sheetRowNumber());
            try {
                var existing = appointmentRepository.findBySheetLocation(
                        sheetName,
                        appointment.sheetRowNumber()
                );
                Appointment saved = appointmentRepository.upsertFromSheet(
                        appointment,
                        sheetName,
                        appointment.sheetRowNumber(),
                        AppointmentJpaEntity.Origin.IMPORT
                );
                if (!saved.id().equals(VisualAgendaSheetParser.text(
                        rows.get(appointment.sheetRowNumber() - 1),
                        6
                ))) {
                    identityWrites.put(
                            sheetName + "!G" + appointment.sheetRowNumber(),
                            List.of(List.of(saved.id()))
                    );
                }
                if (existing.isEmpty()) {
                    created++;
                } else if (sameAppointment(existing.get(), appointment)) {
                    unchanged++;
                } else {
                    updated++;
                }
            } catch (AppointmentException exception) {
                failed++;
                log.error(
                        "Appointment import conflict spreadsheet={} sheet={} row={} reason={}",
                        spreadsheetId,
                        sheetName,
                        appointment.sheetRowNumber(),
                        exception.getMessage()
                );
            }
        }
        sheetsApiClient.batchUpdateValues(spreadsheetId, identityWrites);

        int cancelled = 0;
        if (reconcileDeletes && VisualAgendaSheetParser.hasValidAgendaStructure(rows)) {
            for (Appointment existing : appointmentRepository.findActiveBySheetName(sheetName)) {
                if (existing.sheetRowNumber() != null && !occupiedRows.contains(existing.sheetRowNumber())) {
                    appointmentRepository.delete(existing);
                    cancelled++;
                }
            }
        }
        return new SheetImportResult(created, updated, unchanged, cancelled, failed);
    }

    private boolean sameAppointment(Appointment left, Appointment right) {
        return left.date().equals(right.date())
                && left.time().equals(right.time())
                && left.consultorio() == right.consultorio()
                && java.util.Objects.equals(left.patientName(), right.patientName())
                && java.util.Objects.equals(left.cedula(), right.cedula())
                && java.util.Objects.equals(left.phone(), right.phone())
                && java.util.Objects.equals(left.asistencia(), right.asistencia());
    }

    private boolean isMissingSheet(GoogleSheetsException exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        return message.contains("Unable to parse range")
                || message.contains("not found")
                || message.contains("404");
    }

    public record ImportResult(
            int created,
            int updated,
            int unchanged,
            int cancelled,
            int failed
    ) {
    }

    private record SheetImportResult(
            int created,
            int updated,
            int unchanged,
            int cancelled,
            int failed
    ) {
    }
}
