package com.ocnilentes.mediagenda.modules.appointment.outbound.sheets;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.util.StringUtils;

final class VisualAgendaSheetParser {

    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private VisualAgendaSheetParser() {
    }

    static String sheetName(Consultorio consultorio, LocalDate date) {
        return consultorio.name() + "_" + date.format(DateTimeFormatter.ofPattern("yyyy_MM"));
    }

    static String syntheticId(Consultorio consultorio, LocalDate date, LocalTime time) {
        return consultorio.name() + "|" + date.format(DATE_FORMAT) + "|" + time.format(TIME_FORMAT);
    }

    static Optional<SyntheticId> parseSyntheticId(String id) {
        if (!StringUtils.hasText(id)) {
            return Optional.empty();
        }
        String[] parts = id.split("\\|");
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(new SyntheticId(
                    Consultorio.valueOf(parts[0]),
                    LocalDate.parse(parts[1], DATE_FORMAT),
                    LocalTime.parse(parts[2], TIME_FORMAT)
            ));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    static Optional<SlotLocation> findSlot(
            List<List<Object>> rows,
            LocalDate date,
            LocalTime time
    ) {
        Integer dayStart = findDayMarkerIndex(rows, date);
        if (dayStart == null) {
            return Optional.empty();
        }

        for (int index = dayStart + 2; index < rows.size(); index++) {
            List<Object> row = rows.get(index);
            if (isDayMarker(row) || isBlankRow(row)) {
                break;
            }
            Optional<LocalTime> rowTime = parseTime(text(row, 0));
            if (rowTime.isPresent() && rowTime.get().equals(time)) {
                return Optional.of(new SlotLocation(index + 1, row));
            }
        }
        return Optional.empty();
    }

    static List<Appointment> findAppointmentsInSheet(
            List<List<Object>> rows,
            Consultorio consultorio
    ) {
        List<Appointment> appointments = new ArrayList<>();
        LocalDate currentDate = null;

        for (int index = 0; index < rows.size(); index++) {
            List<Object> row = rows.get(index);
            if (isDayMarker(row)) {
                currentDate = parseDate(text(row, 1)).orElse(null);
                continue;
            }
            if (currentDate == null || isBlankRow(row) || isHeaderRow(row)) {
                continue;
            }

            Optional<LocalTime> time = parseTime(text(row, 0));
            if (time.isEmpty()) {
                continue;
            }

            String name = text(row, 1);
            String cedula = text(row, 2);
            String phone = text(row, 3);
            if (!isOccupied(name, phone)) {
                continue;
            }

            appointments.add(new Appointment(
                    StringUtils.hasText(text(row, 6))
                            ? text(row, 6)
                            : syntheticId(consultorio, currentDate, time.get()),
                    currentDate,
                    time.get(),
                    consultorio,
                    name,
                    cedula,
                    phone,
                    text(row, 5),
                    index + 1
            ));
        }
        return appointments;
    }

    static boolean isOccupied(String name, String phone) {
        if (StringUtils.hasText(phone)) {
            return true;
        }
        if (!StringUtils.hasText(name)) {
            return false;
        }
        return !"ALMUERZO".equalsIgnoreCase(name.trim());
    }

    static boolean isDayMarker(List<Object> row) {
        return "DIA".equalsIgnoreCase(text(row, 0));
    }

    static boolean hasValidAgendaStructure(List<List<Object>> rows) {
        return rows != null && rows.stream()
                .anyMatch(row -> isDayMarker(row) && parseDate(text(row, 1)).isPresent());
    }

    private static Integer findDayMarkerIndex(List<List<Object>> rows, LocalDate date) {
        String expected = date.format(DATE_FORMAT);
        for (int index = 0; index < rows.size(); index++) {
            List<Object> row = rows.get(index);
            if (isDayMarker(row) && expected.equals(text(row, 1))) {
                return index;
            }
        }
        return null;
    }

    private static boolean isHeaderRow(List<Object> row) {
        return "HORA".equalsIgnoreCase(text(row, 0));
    }

    private static boolean isBlankRow(List<Object> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }
        for (Object value : row) {
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return false;
            }
        }
        return true;
    }

    private static Optional<LocalDate> parseDate(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(raw.trim(), DATE_FORMAT));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private static Optional<LocalTime> parseTime(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        String normalized = raw.trim();
        try {
            return Optional.of(LocalTime.parse(normalized, TIME_FORMAT));
        } catch (Exception exception) {
            try {
                return Optional.of(LocalTime.parse(normalized.toLowerCase(Locale.ROOT), DateTimeFormatter.ofPattern("H:mm")));
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }
    }

    static String text(List<Object> row, int index) {
        if (row == null || index >= row.size() || row.get(index) == null) {
            return "";
        }
        return String.valueOf(row.get(index)).trim();
    }

    record SyntheticId(Consultorio consultorio, LocalDate date, LocalTime time) {
    }

    record SlotLocation(int sheetRowNumber, List<Object> row) {
    }
}
