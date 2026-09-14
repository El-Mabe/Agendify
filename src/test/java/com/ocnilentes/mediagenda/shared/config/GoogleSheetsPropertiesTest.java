package com.ocnilentes.mediagenda.shared.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import org.junit.jupiter.api.Test;

class GoogleSheetsPropertiesTest {

    @Test
    void resolvesSeparateConsultorioSpreadsheets() {
        GoogleSheetsProperties properties = new GoogleSheetsProperties(
                true,
                null,
                "id-a",
                "id-b",
                "id-shared",
                null,
                null,
                null,
                "Festivos",
                "Informacion"
        );

        assertEquals("id-a", properties.resolveConsultorioSpreadsheetId(Consultorio.A));
        assertEquals("id-b", properties.resolveConsultorioSpreadsheetId(Consultorio.B));
        assertEquals("id-shared", properties.resolveSharedSpreadsheetId());
    }

    @Test
    void fallsBackToLegacySpreadsheetId() {
        GoogleSheetsProperties properties = new GoogleSheetsProperties(
                true,
                "legacy",
                null,
                null,
                null,
                null,
                null,
                null,
                "Festivos",
                "Informacion"
        );

        assertEquals("legacy", properties.resolveConsultorioSpreadsheetId(Consultorio.A));
        assertEquals("legacy", properties.resolveConsultorioSpreadsheetId(Consultorio.B));
        assertEquals("legacy", properties.resolveSharedSpreadsheetId());
    }

    @Test
    void failsWhenConsultorioSpreadsheetIsMissing() {
        GoogleSheetsProperties properties = new GoogleSheetsProperties(
                true,
                null,
                "id-a",
                null,
                null,
                null,
                null,
                null,
                "Festivos",
                "Informacion"
        );

        assertThrows(
                IllegalStateException.class,
                () -> properties.resolveConsultorioSpreadsheetId(Consultorio.B)
        );
    }
}
