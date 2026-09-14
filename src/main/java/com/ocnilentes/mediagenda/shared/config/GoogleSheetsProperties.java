package com.ocnilentes.mediagenda.shared.config;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "google.sheets")
public record GoogleSheetsProperties(
        boolean enabled,
        /**
         * Legacy: un solo spreadsheet con pestañas A_yyyy_MM y B_yyyy_MM.
         * Si A/B están definidos, este valor queda como fallback.
         */
        String spreadsheetId,
        String spreadsheetIdA,
        String spreadsheetIdB,
        /**
         * Spreadsheet compartido para Festivos e Informacion.
         * Si no se define, se usa A o el legacy.
         */
        String spreadsheetIdShared,
        String credentialsPath,
        String credentialsJson,
        String appointmentsSheet,
        String holidaysSheet,
        String informationSheet
) {
    public String resolveConsultorioSpreadsheetId(Consultorio consultorio) {
        if (consultorio == Consultorio.A && StringUtils.hasText(spreadsheetIdA)) {
            return spreadsheetIdA.trim();
        }
        if (consultorio == Consultorio.B && StringUtils.hasText(spreadsheetIdB)) {
            return spreadsheetIdB.trim();
        }
        if (StringUtils.hasText(spreadsheetId)) {
            return spreadsheetId.trim();
        }
        throw new IllegalStateException(
                "No hay spreadsheet configurado para consultorio " + consultorio
                        + ". Define google.sheets.spreadsheet-id-a/b o spreadsheet-id."
        );
    }

    public String resolveSharedSpreadsheetId() {
        if (StringUtils.hasText(spreadsheetIdShared)) {
            return spreadsheetIdShared.trim();
        }
        if (StringUtils.hasText(spreadsheetIdA)) {
            return spreadsheetIdA.trim();
        }
        if (StringUtils.hasText(spreadsheetId)) {
            return spreadsheetId.trim();
        }
        throw new IllegalStateException(
                "No hay spreadsheet compartido configurado. "
                        + "Define google.sheets.spreadsheet-id-shared, spreadsheet-id-a o spreadsheet-id."
        );
    }

    public boolean hasOperationalSpreadsheet() {
        return StringUtils.hasText(spreadsheetId)
                || (StringUtils.hasText(spreadsheetIdA) && StringUtils.hasText(spreadsheetIdB));
    }
}
