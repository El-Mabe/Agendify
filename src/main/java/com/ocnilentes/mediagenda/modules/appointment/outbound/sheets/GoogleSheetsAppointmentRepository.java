package com.ocnilentes.mediagenda.modules.appointment.outbound.sheets;

import com.ocnilentes.mediagenda.modules.appointment.domain.exception.AppointmentException;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import com.ocnilentes.mediagenda.modules.appointment.domain.port.AppointmentRepository;
import com.ocnilentes.mediagenda.shared.config.GoogleSheetsProperties;
import com.ocnilentes.mediagenda.shared.outbound.google.GoogleSheetsApiClient;
import com.ocnilentes.mediagenda.shared.outbound.google.GoogleSheetsException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class GoogleSheetsAppointmentRepository implements AppointmentRepository {

    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsAppointmentRepository.class);
    private static final long HOLIDAYS_CACHE_TTL_MS = Duration.ofMinutes(15).toMillis();
    private static final long SHEET_CACHE_TTL_MS = Duration.ofMinutes(2).toMillis();

    private final GoogleSheetsApiClient sheetsApiClient;
    private final String holidaysSheet;

    private record CachedSheet(List<List<Object>> data, long expiresAtMs) {
        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAtMs;
        }
    }

    private final Map<String, CachedSheet> sheetCache = new ConcurrentHashMap<>();
    private final Object holidaysLock = new Object();
    private volatile Set<LocalDate> holidaysCache = Set.of();
    private volatile long holidaysCacheExpiresAtMs;

    public GoogleSheetsAppointmentRepository(
            GoogleSheetsApiClient sheetsApiClient,
            GoogleSheetsProperties properties
    ) {
        this.sheetsApiClient = sheetsApiClient;
        this.holidaysSheet = StringUtils.hasText(properties.holidaysSheet())
                ? properties.holidaysSheet()
                : "Festivos";
    }

    @Override
    public Appointment save(Appointment appointment) {
        String sheet = VisualAgendaSheetParser.sheetName(appointment.consultorio(), appointment.date());
        List<List<Object>> rows = loadSheetRequired(sheet);

        VisualAgendaSheetParser.SlotLocation slot = VisualAgendaSheetParser
                .findSlot(rows, appointment.date(), appointment.time())
                .orElseThrow(() -> new AppointmentException(
                        "No se encontró el horario " + appointment.time()
                                + " del día " + appointment.date()
                                + " en la pestaña " + sheet
                                + ". Verifica que recepción haya creado el bloque DIA."
                ));

        String name = VisualAgendaSheetParser.text(slot.row(), 1);
        String phone = VisualAgendaSheetParser.text(slot.row(), 3);
        if (VisualAgendaSheetParser.isOccupied(name, phone)) {
            throw new AppointmentException("Ese horario acabó de ocuparse. Elige otra hora.");
        }

        String range = sheet + "!B" + slot.sheetRowNumber() + ":F" + slot.sheetRowNumber();
        sheetsApiClient.updateValues(
                range,
                List.of(List.of(
                        appointment.patientName(),
                        appointment.cedula() == null ? "" : appointment.cedula(),
                        appointment.phone(),
                        "",
                        ""
                ))
        );
        invalidate(sheet);

        return new Appointment(
                VisualAgendaSheetParser.syntheticId(
                        appointment.consultorio(),
                        appointment.date(),
                        appointment.time()
                ),
                appointment.date(),
                appointment.time(),
                appointment.consultorio(),
                appointment.patientName(),
                appointment.cedula(),
                appointment.phone(),
                appointment.asistencia(),
                slot.sheetRowNumber()
        );
    }

    @Override
    public void updateAsistencia(Appointment appointment, String asistencia) {
        String sheet = VisualAgendaSheetParser.sheetName(appointment.consultorio(), appointment.date());
        List<List<Object>> rows = loadSheetRequired(sheet);

        int rowNumber = appointment.sheetRowNumber() != null
                ? appointment.sheetRowNumber()
                : VisualAgendaSheetParser.findSlot(rows, appointment.date(), appointment.time())
                .map(VisualAgendaSheetParser.SlotLocation::sheetRowNumber)
                .orElseThrow(() -> new AppointmentException("No se encontró la cita en la agenda visual"));

        String range = sheet + "!F" + rowNumber;
        sheetsApiClient.updateValues(range, List.of(List.of(asistencia == null ? "" : asistencia)));
        invalidate(sheet);
    }

    @Override
    public List<Appointment> findByPhone(String phone) {
        return findAppointmentsInRange(appointment -> appointment.phone().equals(phone));
    }

    @Override
    public List<Appointment> findByCedula(String cedula) {
        return findAppointmentsInRange(appointment ->
                appointment.cedula() != null && appointment.cedula().equals(cedula)
        );
    }

    private List<Appointment> findAppointmentsInRange(
            java.util.function.Predicate<Appointment> filter
    ) {
        List<Appointment> result = new ArrayList<>();
        LocalDate start = LocalDate.now().minusMonths(1);
        LocalDate end = LocalDate.now().plusMonths(3);

        for (YearMonth month = YearMonth.from(start); !month.isAfter(YearMonth.from(end)); month = month.plusMonths(1)) {
            LocalDate probe = month.atDay(1);
            for (Consultorio consultorio : Consultorio.values()) {
                Optional<List<List<Object>>> rows = loadSheetOptional(
                        VisualAgendaSheetParser.sheetName(consultorio, probe)
                );
                rows.ifPresent(value -> result.addAll(
                        VisualAgendaSheetParser.findAppointmentsInSheet(value, consultorio).stream()
                                .filter(filter)
                                .toList()
                ));
            }
        }
        return result;
    }

    @Override
    public List<Appointment> findByDate(LocalDate date) {
        List<Appointment> result = new ArrayList<>();
        for (Consultorio consultorio : Consultorio.values()) {
            Optional<List<List<Object>>> rows = loadSheetOptional(
                    VisualAgendaSheetParser.sheetName(consultorio, date)
            );
            rows.ifPresent(value -> result.addAll(
                    VisualAgendaSheetParser.findAppointmentsInSheet(value, consultorio).stream()
                            .filter(appointment -> appointment.date().equals(date))
                            .toList()
            ));
        }
        return result;
    }

    @Override
    public Optional<Appointment> findById(String id) {
        return VisualAgendaSheetParser.parseSyntheticId(id).flatMap(syntheticId -> {
            Optional<List<List<Object>>> rows = loadSheetOptional(
                    VisualAgendaSheetParser.sheetName(syntheticId.consultorio(), syntheticId.date())
            );
            if (rows.isEmpty()) {
                return Optional.empty();
            }
            return VisualAgendaSheetParser.findAppointmentsInSheet(rows.get(), syntheticId.consultorio()).stream()
                    .filter(appointment -> appointment.id().equals(id))
                    .findFirst();
        });
    }

    @Override
    public boolean existsSlot(LocalDate date, LocalTime time, Consultorio consultorio) {
        Optional<List<List<Object>>> rows = loadSheetOptional(
                VisualAgendaSheetParser.sheetName(consultorio, date)
        );
        if (rows.isEmpty()) {
            return true;
        }

        return VisualAgendaSheetParser.findSlot(rows.get(), date, time)
                .map(slot -> {
                    String name = VisualAgendaSheetParser.text(slot.row(), 1);
                    String phone = VisualAgendaSheetParser.text(slot.row(), 3);
                    return VisualAgendaSheetParser.isOccupied(name, phone);
                })
                .orElse(true);
    }

    @Override
    public long countByDateAndConsultorio(LocalDate date, Consultorio consultorio) {
        Optional<List<List<Object>>> rows = loadSheetOptional(
                VisualAgendaSheetParser.sheetName(consultorio, date)
        );
        if (rows.isEmpty()) {
            return 0;
        }
        return VisualAgendaSheetParser.findAppointmentsInSheet(rows.get(), consultorio).stream()
                .filter(appointment -> appointment.date().equals(date))
                .count();
    }

    @Override
    public void delete(Appointment appointment) {
        String sheet = VisualAgendaSheetParser.sheetName(appointment.consultorio(), appointment.date());
        List<List<Object>> rows = loadSheetRequired(sheet);

        int rowNumber = appointment.sheetRowNumber() != null
                ? appointment.sheetRowNumber()
                : VisualAgendaSheetParser.findSlot(rows, appointment.date(), appointment.time())
                .map(VisualAgendaSheetParser.SlotLocation::sheetRowNumber)
                .orElseThrow(() -> new AppointmentException("No se encontró la cita en la agenda visual"));

        String range = sheet + "!B" + rowNumber + ":F" + rowNumber;
        sheetsApiClient.updateValues(range, List.of(List.of("", "", "", "", "")));
        invalidate(sheet);
    }

    @Override
    public Set<LocalDate> findHolidays() {
        long now = System.currentTimeMillis();
        if (now < holidaysCacheExpiresAtMs) {
            return holidaysCache;
        }

        synchronized (holidaysLock) {
            long refreshedNow = System.currentTimeMillis();
            if (refreshedNow < holidaysCacheExpiresAtMs) {
                return holidaysCache;
            }
            Set<LocalDate> holidays = loadHolidaysFromSheet();
            holidaysCache = Set.copyOf(holidays);
            holidaysCacheExpiresAtMs = refreshedNow + HOLIDAYS_CACHE_TTL_MS;
            return holidaysCache;
        }
    }

    private Set<LocalDate> loadHolidaysFromSheet() {
        List<List<Object>> values = sheetsApiClient.getValues(holidaysSheet + "!A2:A");
        Set<LocalDate> holidays = new HashSet<>();
        for (List<Object> row : values) {
            if (row == null || row.isEmpty() || row.getFirst() == null) {
                continue;
            }
            String raw = String.valueOf(row.getFirst()).trim();
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            try {
                holidays.add(LocalDate.parse(raw, VisualAgendaSheetParser.DATE_FORMAT));
            } catch (Exception ignored) {
                // skip invalid rows
            }
        }
        return holidays;
    }

    private List<List<Object>> loadSheetRequired(String sheetName) {
        return loadSheetOptional(sheetName).orElseThrow(() -> new AppointmentException(
                "Agenda del mes no disponible (" + sheetName + "). "
                        + "Pide a recepción crear esa pestaña desde la plantilla."
        ));
    }

    private Optional<List<List<Object>>> loadSheetOptional(String sheetName) {
        CachedSheet cached = sheetCache.get(sheetName);
        if (cached != null && !cached.isExpired()) {
            return Optional.of(cached.data());
        }

        try {
            log.debug("Reading sheet from Google Sheets API: {}", sheetName);
            List<List<Object>> values = sheetsApiClient.getValues(sheetName + "!A1:F");
            sheetCache.put(sheetName, new CachedSheet(values, System.currentTimeMillis() + SHEET_CACHE_TTL_MS));
            return Optional.of(values);
        } catch (GoogleSheetsException exception) {
            String body = exception.getMessage() == null ? "" : exception.getMessage();
            if (body.contains("Unable to parse range") || body.contains("404") || body.contains("not found")) {
                return Optional.empty();
            }
            // En 429 devolver caché vieja si existe, en lugar de propagar el error
            if (body.contains("429") || body.contains("RATE_LIMIT") || body.contains("RESOURCE_EXHAUSTED")) {
                log.warn("Google Sheets rate limit for sheet={}, using stale cache={}", sheetName, cached != null);
                if (cached != null) {
                    return Optional.of(cached.data());
                }
            }
            throw exception;
        }
    }

    private void invalidate(String sheetName) {
        sheetCache.remove(sheetName);
    }
}
