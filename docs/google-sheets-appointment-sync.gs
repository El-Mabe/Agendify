/**
 * Sincroniza cambios manuales de recepción hacia PostgreSQL.
 *
 * Instalar ESTE script en CADA spreadsheet de consultorio (A y B).
 *
 * Propiedades del proyecto (Apps Script → Configuración del proyecto → Propiedades):
 * - MEDIAGENDA_API_URL=https://tu-servicio.up.railway.app
 * - SHEET_SYNC_TOKEN=mismo valor que APPOINTMENT_SHEET_SYNC_TOKEN en Railway
 * - CONSULTORIO=A   (o B en el otro archivo)
 *
 * Activador instalable:
 * función onAppointmentEdit → evento "Al editar"
 *
 * Prueba manual: selecciona una celda de la fila de la cita y ejecuta testSyncSelectedRow.
 */
function onAppointmentEdit(event) {
  if (!event || !event.range) {
    return;
  }

  const sheet = event.range.getSheet();
  const sheetName = sheet.getName();
  const match = sheetName.match(/^([AB])_(\d{4})_(\d{2})$/);
  if (!match) {
    console.log('Skip sync: nombre de hoja inválido "' + sheetName + '" (esperado A_YYYY_MM)');
    return;
  }

  const expectedConsultorio = String(
    PropertiesService.getScriptProperties().getProperty('CONSULTORIO') || ''
  ).toUpperCase();
  if (expectedConsultorio && expectedConsultorio !== match[1]) {
    throw new Error(
      'Esta hoja pertenece al consultorio ' + match[1] +
      ' pero el script está configurado como ' + expectedConsultorio
    );
  }

  const column = event.range.getColumn();
  if (column > 6 || event.range.getLastColumn() < 2) {
    console.log(
      'Skip sync: edición fuera de columnas B-F (columna=' +
      column +
      ', lastColumn=' +
      event.range.getLastColumn() +
      ')'
    );
    return;
  }

  for (let row = event.range.getRow(); row <= event.range.getLastRow(); row++) {
    syncAppointmentRow_(sheet, sheetName, match[1], row);
  }
}

/** Ejecutar desde el editor con una celda de la fila de cita seleccionada. */
function testSyncSelectedRow() {
  const range = SpreadsheetApp.getActiveRange();
  if (!range) {
    throw new Error('Selecciona una celda de la fila de la cita e inténtalo de nuevo');
  }
  const sheet = range.getSheet();
  const sheetName = sheet.getName();
  const match = sheetName.match(/^([AB])_(\d{4})_(\d{2})$/);
  if (!match) {
    throw new Error('La pestaña debe llamarse A_YYYY_MM o B_YYYY_MM. Actual: ' + sheetName);
  }
  syncAppointmentRow_(sheet, sheetName, match[1], range.getRow());
}

function syncAppointmentRow_(sheet, sheetName, consultorio, row) {
  // Usar el texto visible evita el desfase de zona horaria de getValue() (Date).
  // Ej.: 11:00 en hoja no debe guardarse como 06:00 en Postgres.
  const time = formatTime_(sheet.getRange(row, 1).getDisplayValue());
  if (!time) {
    console.log('Skip sync fila ' + row + ': sin hora válida en columna A');
    return;
  }

  const date = findDateForRow_(sheet, row);
  if (!date) {
    console.log('Skip sync fila ' + row + ': no se encontró marcador DIA arriba');
    return;
  }

  const values = sheet.getRange(row, 2, 1, 6).getDisplayValues()[0];
  const properties = PropertiesService.getScriptProperties();
  const apiUrl = properties.getProperty('MEDIAGENDA_API_URL');
  const token = properties.getProperty('SHEET_SYNC_TOKEN');
  if (!apiUrl || !token) {
    throw new Error('Configura MEDIAGENDA_API_URL y SHEET_SYNC_TOKEN en las propiedades del script');
  }

  const payload = {
    eventId: Utilities.getUuid(),
    appointmentId: values[5] || null,
    sheetName: sheetName,
    rowNumber: row,
    date: date,
    time: time,
    consultorio: consultorio,
    patientName: values[0],
    cedula: values[1],
    phone: values[2],
    asistencia: values[4]
  };

  console.log('Sync POST fila ' + row + ': ' + JSON.stringify(payload));

  const response = UrlFetchApp.fetch(
    apiUrl.replace(/\/$/, '') + '/api/v1/admin/appointments/sheets/row',
    {
      method: 'post',
      contentType: 'application/json',
      headers: {'X-Sheet-Sync-Token': token},
      payload: JSON.stringify(payload),
      muteHttpExceptions: true
    }
  );

  const code = response.getResponseCode();
  const body = response.getContentText();
  console.log('Sync response fila ' + row + ': HTTP ' + code + ' ' + body);

  if (code < 200 || code >= 300) {
    throw new Error('MediAgenda sync failed: ' + code + ' ' + body);
  }

  const result = JSON.parse(body);
  if (result.action === 'CANCELLED' || result.action === 'IGNORED_EMPTY') {
    sheet.getRange(row, 7).clearContent();
  } else if (result.appointmentId) {
    sheet.getRange(row, 7).setValue(result.appointmentId);
  }

  if (result.action === 'IGNORED_EMPTY') {
    throw new Error(
      'Fila ' + row + ' ignorada: falta NOMBRE o TELÉFONO (solo cédula no crea cita)'
    );
  }
}

function findDateForRow_(sheet, row) {
  for (let current = row; current >= 1; current--) {
    const marker = String(sheet.getRange(current, 1).getDisplayValue()).trim().toUpperCase();
    if (marker === 'DIA') {
      const display = String(sheet.getRange(current, 2).getDisplayValue()).trim();
      if (/^\d{4}-\d{2}-\d{2}$/.test(display)) {
        return display;
      }
      const value = sheet.getRange(current, 2).getValue();
      if (value instanceof Date) {
        // Misma lógica que la hora: componentes de calendario sin desfase TZ.
        const yyyy = value.getFullYear();
        const mm = String(value.getMonth() + 1).padStart(2, '0');
        const dd = String(value.getDate()).padStart(2, '0');
        return yyyy + '-' + mm + '-' + dd;
      }
      return null;
    }
  }
  return null;
}

function formatTime_(value) {
  if (value instanceof Date) {
    // Componentes UTC = hora "de pared" que Sheets guarda en celdas de hora.
    const hh = String(value.getUTCHours()).padStart(2, '0');
    const mm = String(value.getUTCMinutes()).padStart(2, '0');
    const ss = String(value.getUTCSeconds()).padStart(2, '0');
    return hh + ':' + mm + ':' + ss;
  }
  const text = String(value || '').trim();
  const match = text.match(/^(\d{1,2}):(\d{2})(?::(\d{2}))?/);
  if (!match) {
    return null;
  }
  return String(match[1]).padStart(2, '0') + ':' + match[2] + ':' + (match[3] || '00');
}
