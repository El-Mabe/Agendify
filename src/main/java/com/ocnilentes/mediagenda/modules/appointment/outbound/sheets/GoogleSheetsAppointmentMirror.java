package com.ocnilentes.mediagenda.modules.appointment.outbound.sheets;

import com.ocnilentes.mediagenda.modules.appointment.domain.exception.AppointmentException;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.shared.config.GoogleSheetsProperties;
import com.ocnilentes.mediagenda.shared.outbound.google.GoogleSheetsApiClient;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "google.sheets", name = "enabled", havingValue = "true")
public class GoogleSheetsAppointmentMirror {

    private final GoogleSheetsApiClient sheetsApiClient;
    private final GoogleSheetsProperties sheetsProperties;

    public GoogleSheetsAppointmentMirror(
            GoogleSheetsApiClient sheetsApiClient,
            GoogleSheetsProperties sheetsProperties
    ) {
        this.sheetsApiClient = sheetsApiClient;
        this.sheetsProperties = sheetsProperties;
    }

    public SheetLocation upsert(Appointment appointment) {
        String spreadsheetId = sheetsProperties.resolveConsultorioSpreadsheetId(appointment.consultorio());
        String sheetName = VisualAgendaSheetParser.sheetName(appointment.consultorio(), appointment.date());
        List<List<Object>> rows = loadSheet(spreadsheetId, sheetName);
        VisualAgendaSheetParser.SlotLocation slot = locate(rows, appointment);

        String currentPhone = VisualAgendaSheetParser.text(slot.row(), 3);
        if (VisualAgendaSheetParser.isOccupied(
                VisualAgendaSheetParser.text(slot.row(), 1),
                currentPhone
        ) && !belongsToAppointment(slot.row(), appointment)) {
            throw new AppointmentException(
                    "El horario está ocupado en Google Sheets por otra cita. Se requiere revisión de recepción."
            );
        }

        sheetsApiClient.updateValues(
                spreadsheetId,
                sheetName + "!B" + slot.sheetRowNumber() + ":G" + slot.sheetRowNumber(),
                List.of(List.of(
                        appointment.patientName(),
                        appointment.cedula() == null ? "" : appointment.cedula(),
                        appointment.phone(),
                        VisualAgendaSheetParser.text(slot.row(), 4),
                        appointment.asistencia() == null ? "" : appointment.asistencia(),
                        appointment.id()
                ))
        );
        return new SheetLocation(spreadsheetId, sheetName, slot.sheetRowNumber());
    }

    public void delete(Appointment appointment) {
        String spreadsheetId = sheetsProperties.resolveConsultorioSpreadsheetId(appointment.consultorio());
        String sheetName = VisualAgendaSheetParser.sheetName(appointment.consultorio(), appointment.date());
        VisualAgendaSheetParser.SlotLocation slot = locate(loadSheet(spreadsheetId, sheetName), appointment);
        if (!belongsToAppointment(slot.row(), appointment)) {
            if (!VisualAgendaSheetParser.isOccupied(
                    VisualAgendaSheetParser.text(slot.row(), 1),
                    VisualAgendaSheetParser.text(slot.row(), 3)
            )) {
                return;
            }
            throw new AppointmentException("No se eliminó la fila porque pertenece a otra cita.");
        }
        sheetsApiClient.updateValues(
                spreadsheetId,
                sheetName + "!B" + slot.sheetRowNumber() + ":G" + slot.sheetRowNumber(),
                List.of(List.of("", "", "", "", "", ""))
        );
    }

    public SheetLocation updateAttendance(Appointment appointment) {
        String spreadsheetId = sheetsProperties.resolveConsultorioSpreadsheetId(appointment.consultorio());
        String sheetName = VisualAgendaSheetParser.sheetName(appointment.consultorio(), appointment.date());
        VisualAgendaSheetParser.SlotLocation slot = locate(loadSheet(spreadsheetId, sheetName), appointment);
        if (!belongsToAppointment(slot.row(), appointment)) {
            throw new AppointmentException("No se actualizó la asistencia porque la fila pertenece a otra cita.");
        }
        sheetsApiClient.updateValues(
                spreadsheetId,
                sheetName + "!F" + slot.sheetRowNumber(),
                List.of(List.of(appointment.asistencia() == null ? "" : appointment.asistencia()))
        );
        return new SheetLocation(spreadsheetId, sheetName, slot.sheetRowNumber());
    }

    private List<List<Object>> loadSheet(String spreadsheetId, String sheetName) {
        return sheetsApiClient.getValues(spreadsheetId, sheetName + "!A1:G");
    }

    private VisualAgendaSheetParser.SlotLocation locate(
            List<List<Object>> rows,
            Appointment appointment
    ) {
        // 1) ID dentro del día correcto (evita escribir CONFIRMO en otra fecha si el UUID se repitió).
        Optional<VisualAgendaSheetParser.SlotLocation> byIdInDay =
                VisualAgendaSheetParser.findByAppointmentId(rows, appointment.date(), appointment.id());
        if (byIdInDay.isPresent()) {
            return byIdInDay.get();
        }

        // 2) Slot exacto fecha + hora.
        Optional<VisualAgendaSheetParser.SlotLocation> bySlot =
                VisualAgendaSheetParser.findSlot(rows, appointment.date(), appointment.time());
        if (bySlot.isPresent()) {
            return bySlot.get();
        }

        // 3) Misma cédula/teléfono en ese día (mitiga desfase de hora 11:00 vs 06:00).
        Optional<VisualAgendaSheetParser.SlotLocation> byPatient =
                VisualAgendaSheetParser.findByPatientInDay(
                        rows,
                        appointment.date(),
                        appointment.cedula(),
                        appointment.phone()
                );
        if (byPatient.isPresent()) {
            return byPatient.get();
        }

        // 4) Último recurso: primer ID en toda la hoja (puede ser fecha incorrecta).
        for (int index = 0; index < rows.size(); index++) {
            if (sameValue(VisualAgendaSheetParser.text(rows.get(index), 6), appointment.id())) {
                return new VisualAgendaSheetParser.SlotLocation(index + 1, rows.get(index));
            }
        }

        throw new AppointmentException(
                "No se encontró el horario " + appointment.time()
                        + " del día " + appointment.date()
        );
    }

    private boolean belongsToAppointment(List<Object> row, Appointment appointment) {
        String currentId = VisualAgendaSheetParser.text(row, 6);
        if (StringUtils.hasText(currentId)) {
            return sameValue(currentId, appointment.id());
        }
        return sameValue(VisualAgendaSheetParser.text(row, 2), appointment.cedula())
                && sameValue(VisualAgendaSheetParser.text(row, 3), appointment.phone());
    }

    private boolean sameValue(String left, String right) {
        return StringUtils.hasText(left) && StringUtils.hasText(right) && left.trim().equals(right.trim());
    }

    public record SheetLocation(String spreadsheetId, String sheetName, int rowNumber) {
    }
}
