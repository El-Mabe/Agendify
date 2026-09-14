/**
 * Genera la agenda visual de UN consultorio para un año completo.
 *
 * Flujo recomendado (archivo aparte por consultorio):
 * 1. Crea spreadsheet "Agenda Consultorio A" → pega este script → CONSULTORIO = 'A'
 *    → createYearAgenda_()
 * 2. Crea spreadsheet "Agenda Consultorio B" → pega este script → CONSULTORIO = 'B'
 *    → createYearAgenda_()
 * 3. Opcional: spreadsheet "Agenda Compartida" con Festivos + Informacion
 *    → createSharedSupportSheets_()
 *
 * Pestañas generadas: A_yyyy_MM o B_yyyy_MM (el prefijo identifica el consultorio
 * aunque cada archivo esté separado).
 */

var YEAR = 2026;

/** 'A' o 'B' */
var CONSULTORIO = 'A';

var INCLUDE_SUPPORT_SHEETS = false;

var CONSULTORIO_A = {
  morningStart: [8, 0],
  morningEnd: [12, 0],
  lunch: ['12:00', '12:20', '12:40'],
  afternoonStart: [13, 0],
  afternoonEnd: [17, 0]
};

var CONSULTORIO_B = {
  morningStart: [9, 0],
  morningEnd: [13, 0],
  lunch: ['13:00', '13:20', '13:40'],
  afternoonStart: [14, 0],
  afternoonEnd: [18, 0]
};

var DAY_NAMES = [
  'DOMINGO', 'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO'
];

function createYearAgenda_() {
  var spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
  var consultorio = String(CONSULTORIO || '').toUpperCase();
  if (consultorio !== 'A' && consultorio !== 'B') {
    throw new Error("CONSULTORIO debe ser 'A' o 'B'");
  }

  var schedule = consultorio === 'A' ? CONSULTORIO_A : CONSULTORIO_B;
  if (INCLUDE_SUPPORT_SHEETS) {
    createSupportSheets_(spreadsheet);
  }

  for (var month = 1; month <= 12; month++) {
    createMonthSheet_(spreadsheet, consultorio, month, schedule);
  }

  SpreadsheetApp.getUi().alert(
    'Agenda ' + YEAR + ' consultorio ' + consultorio + ' creada.\n' +
    'Comparte este archivo con la service account (Editor).'
  );
}

function createSharedSupportSheets_() {
  createSupportSheets_(SpreadsheetApp.getActiveSpreadsheet());
  SpreadsheetApp.getUi().alert('Pestañas Festivos e Informacion listas.');
}

function createSupportSheets_(spreadsheet) {
  var festivos = getOrCreateSheet_(spreadsheet, 'Festivos');
  festivos.clear();
  festivos.getRange(1, 1).setValue('fecha');
  festivos.getRange(2, 1, 5, 1).setValues([
    [YEAR + '-01-01'],
    [YEAR + '-05-01'],
    [YEAR + '-07-20'],
    [YEAR + '-08-07'],
    [YEAR + '-12-25']
  ]);

  var info = getOrCreateSheet_(spreadsheet, 'Informacion');
  info.clear();
  info.getRange(1, 1, 1, 2).setValues([['clave', 'valor']]);
  info.getRange(2, 1, 1, 2).setValues([[
    'mensaje',
    'Somos una óptica en Cali.\n\nEscribe *menú* para volver a las opciones.'
  ]]);
}

function createMonthSheet_(spreadsheet, consultorio, month, schedule) {
  var sheetName = consultorio + '_' + YEAR + '_' + pad2_(month);
  var sheet = getOrCreateSheet_(spreadsheet, sheetName);
  sheet.clear();

  var rows = [];
  var daysInMonth = new Date(YEAR, month, 0).getDate();

  for (var day = 1; day <= daysInMonth; day++) {
    var date = new Date(YEAR, month - 1, day);
    if (date.getDay() === 0) {
      continue;
    }

    rows.push(['DIA', formatDate_(date), DAY_NAMES[date.getDay()], '', '', '', '']);
    rows.push(['HORA', 'NOMBRE', 'CEDULA', 'TELEFONO', 'OBSERVACION', 'ASISTENCIA', 'APPOINTMENT_ID']);

    appendSlots_(rows, schedule.morningStart, schedule.morningEnd);
    schedule.lunch.forEach(function (time) {
      rows.push([time, 'ALMUERZO', '', '', '', '', '']);
    });
    appendSlots_(rows, schedule.afternoonStart, schedule.afternoonEnd);
    rows.push(['', '', '', '', '', '', '']);
  }

  if (rows.length === 0) {
    return;
  }

  sheet.getRange(1, 1, rows.length, 7).setValues(rows);
  sheet.hideColumns(7);
  sheet.setFrozenRows(0);
  sheet.autoResizeColumns(1, 6);
}

function appendSlots_(rows, startHm, endHm) {
  var current = new Date(2000, 0, 1, startHm[0], startHm[1], 0);
  var end = new Date(2000, 0, 1, endHm[0], endHm[1], 0);
  while (current < end) {
    rows.push([formatTime_(current), '', '', '', '', '', '']);
    current = new Date(current.getTime() + 20 * 60 * 1000);
  }
}

function getOrCreateSheet_(spreadsheet, name) {
  var sheet = spreadsheet.getSheetByName(name);
  if (sheet) {
    return sheet;
  }
  return spreadsheet.insertSheet(name);
}

function formatDate_(date) {
  return Utilities.formatDate(date, Session.getScriptTimeZone(), 'yyyy-MM-dd');
}

function formatTime_(date) {
  return Utilities.formatDate(date, Session.getScriptTimeZone(), 'HH:mm');
}

function pad2_(value) {
  return value < 10 ? '0' + value : String(value);
}
