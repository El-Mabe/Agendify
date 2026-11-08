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

/** Encabezado de cada día (como la agenda visual del cliente). */
var DAY_HEADER_COLORS = {
  LUNES: { background: '#6D9EEB', font: '#FFFFFF' },
  MARTES: { background: '#B55F04', font: '#FFFFFF' },
  MIERCOLES: { background: '#FFD966', font: '#20124D' },
  JUEVES: { background: '#EA9999', font: '#20124D' },
  VIERNES: { background: '#999999', font: '#FFFFFF' },
  SABADO: { background: '#A64D79', font: '#FFFFFF' }
};

var COLUMN_WIDTHS = [100, 200, 150, 160, 220, 150];
var BODY_FONT_SIZE = 11;
var HEADER_FONT_SIZE = 12;
var DAY_FONT_SIZE = 14;
var ALMUERZO_BACKGROUND = '#E06666';
var ALMUERZO_FONT = '#20124D';

/**
 * Desplegable de ASISTENCIA. Vacío permitido; texto libre rechazado.
 * Ejecutar applyAttendanceDropdownRemainingMonths_() en cada spreadsheet
 * (A y B) para los meses que aún no han pasado. No borra citas.
 */
var ASISTENCIA_OPTIONS = ['CONFIRMO', 'ASISTIO', 'NO ASISTIO'];

var ASISTENCIA_ROW_COLORS = [
  { values: ['CONFIRMO', 'CONFIRMADO'], background: '#FFCC00', font: '#20124D' },
  { values: ['ASISTIO'], background: '#04FF02', font: '#20124D' },
  { values: ['NO ASISTIO'], background: '#CC3300', font: '#FFFFFF' }
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

    var morningEnd = schedule.morningEnd;
    var lunch = schedule.lunch.slice();
    var afternoonStart = schedule.afternoonStart;
    if (consultorio === 'B' && date.getDay() === 6) {
      morningEnd = [12, 0];
      lunch = ['12:00', '12:20', '12:40'];
      afternoonStart = [13, 0];
    }
    appendSlots_(rows, schedule.morningStart, morningEnd);
    lunch.forEach(function (time) {
      rows.push([time, 'ALMUERZO', '', '', '', '', '']);
    });
    appendSlots_(rows, afternoonStart, schedule.afternoonEnd);
    rows.push(['', '', '', '', '', '', '']);
  }

  if (rows.length === 0) {
    return;
  }

  sheet.getRange(1, 1, rows.length, 7).setValues(rows);
  sheet.hideColumns(7);
  sheet.setFrozenRows(0);
  applyVisualStyleToSheet_(sheet);
  applyAttendanceDropdownToSheet_(sheet);
  applyAttendanceRowColorsToSheet_(sheet);
}

/**
 * Colores de encabezado por día, columnas más anchas y letra más grande.
 * No borra citas. Correr en Consultorio A y B para los meses que restan.
 */
function applyDayHeaderStylesRemainingMonths_() {
  var spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
  var now = new Date();
  var startMonth = now.getFullYear() === YEAR ? now.getMonth() + 1 : 1;
  var processed = [];

  spreadsheet.getSheets().forEach(function (sheet) {
    var match = /^([AB])_(\d{4})_(\d{2})$/.exec(sheet.getName());
    if (!match) {
      return;
    }
    var year = parseInt(match[2], 10);
    var month = parseInt(match[3], 10);
    if (year !== YEAR || month < startMonth) {
      return;
    }
    applyVisualStyleToSheet_(sheet);
    applyAttendanceRowColorsToSheet_(sheet);
    processed.push(sheet.getName());
  });

  SpreadsheetApp.getUi().alert(
    processed.length
      ? 'Estilos de encabezado aplicados en:\n' + processed.join('\n')
      : 'No se encontraron pestañas ' + YEAR + ' del mes ' + startMonth + ' en adelante.'
  );
}

function applyVisualStyleToSheet_(sheet) {
  var lastRow = sheet.getLastRow();
  var lastCol = 6;
  if (lastRow < 1) {
    return;
  }

  var used = sheet.getRange(1, 1, lastRow, lastCol);
  used.setFontFamily('Arial');
  used.setFontSize(BODY_FONT_SIZE);
  used.setVerticalAlignment('middle');
  used.setWrap(true);

  for (var col = 0; col < COLUMN_WIDTHS.length; col++) {
    sheet.setColumnWidth(col + 1, COLUMN_WIDTHS[col]);
  }
  sheet.setRowHeights(1, lastRow, 24);

  try {
    sheet.getRange(1, 1, lastRow, lastCol).breakApart();
  } catch (ignored) {
  }

  var values = sheet.getRange(1, 1, lastRow, 3).getDisplayValues();
  for (var i = 0; i < values.length; i++) {
    var marker = String(values[i][0] || '').trim().toUpperCase();
    if (marker !== 'DIA') {
      continue;
    }

    var dayName = String(values[i][2] || '').trim().toUpperCase()
      .replace('Á', 'A')
      .replace('É', 'E')
      .replace('Í', 'I')
      .replace('Ó', 'O')
      .replace('Ú', 'U');
    var colors = DAY_HEADER_COLORS[dayName];
    if (!colors) {
      continue;
    }

    var dayRow = i + 1;
    var banner = sheet.getRange(dayRow, 1, 1, lastCol);
    banner.setBackground(colors.background);
    banner.setFontColor(colors.font);
    banner.setFontWeight('bold');
    banner.setFontSize(DAY_FONT_SIZE);
    banner.setHorizontalAlignment('center');
    sheet.setRowHeight(dayRow, 34);

    if (lastCol >= 3) {
      sheet.getRange(dayRow, 3, 1, lastCol - 2).merge();
    }

    var headerRow = dayRow + 1;
    var headerLabel = headerRow <= lastRow
      ? String(values[i + 1][0] || '').trim().toUpperCase()
      : '';
    if (headerLabel === 'HORA') {
      var header = sheet.getRange(headerRow, 1, 1, lastCol);
      header.setBackground(lightenHex_(colors.background, 0.35));
      header.setFontColor('#20124D');
      header.setFontWeight('bold');
      header.setFontSize(HEADER_FONT_SIZE);
      header.setHorizontalAlignment('center');
      sheet.setRowHeight(headerRow, 28);
    }
  }
  applyAlmuerzoStyleToSheet_(sheet);
}

function lightenHex_(hex, amount) {
  var raw = String(hex || '').replace('#', '');
  if (raw.length !== 6) {
    return hex;
  }
  var parts = [];
  for (var i = 0; i < 6; i += 2) {
    var value = parseInt(raw.substring(i, i + 2), 16);
    value = Math.round(value + (255 - value) * amount);
    var out = value.toString(16).toUpperCase();
    parts.push(out.length < 2 ? '0' + out : out);
  }
  return '#' + parts.join('');
}

/**
 * Aplica el desplegable CONFIRMO / ASISTIO / NO ASISTIO en las pestañas
 * del mes actual en adelante (YEAR). Correr una vez en Consultorio A y
 * otra en Consultorio B. No usa clear(); no toca NOMBRE/CEDULA/citas.
 */
function applyAttendanceDropdownRemainingMonths_() {
  var spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
  var now = new Date();
  var startMonth = now.getFullYear() === YEAR ? now.getMonth() + 1 : 1;
  var processed = [];

  spreadsheet.getSheets().forEach(function (sheet) {
    var match = /^([AB])_(\d{4})_(\d{2})$/.exec(sheet.getName());
    if (!match) {
      return;
    }
    var year = parseInt(match[2], 10);
    var month = parseInt(match[3], 10);
    if (year !== YEAR || month < startMonth) {
      return;
    }
    applyAttendanceDropdownToSheet_(sheet);
    applyAttendanceRowColorsToSheet_(sheet);
    processed.push(sheet.getName());
  });

  SpreadsheetApp.getUi().alert(
    processed.length
      ? 'Desplegable ASISTENCIA aplicado en:\n' + processed.join('\n')
      : 'No se encontraron pestañas ' + YEAR + ' del mes ' + startMonth + ' en adelante.'
  );
}

/**
 * Sábado en B: almuerzo solo 12:00-12:40. 13:00-13:40 dejan de ser ALMUERZO
 * (pasan a huecos agendables si no hay cita). No pisa pacientes. Correr en B.
 */
function applySaturdayLunchConsultorioBRemainingMonths_() {
  var spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
  var now = new Date();
  var startMonth = now.getFullYear() === YEAR ? now.getMonth() + 1 : 1;
  var processed = [];

  spreadsheet.getSheets().forEach(function (sheet) {
    var match = /^B_(\d{4})_(\d{2})$/.exec(sheet.getName());
    if (!match) {
      return;
    }
    var year = parseInt(match[1], 10);
    var month = parseInt(match[2], 10);
    if (year !== YEAR || month < startMonth) {
      return;
    }
    var updated = markSaturdayLunchB_(sheet);
    applyAttendanceDropdownToSheet_(sheet);
    applyAttendanceRowColorsToSheet_(sheet);
    processed.push(sheet.getName() + ' (' + updated + ' filas)');
  });

  SpreadsheetApp.getUi().alert(
    processed.length
      ? 'Almuerzo sábado 12-1 (B) aplicado en:\n' + processed.join('\n')
      : 'No se encontraron pestañas B_' + YEAR + ' del mes ' + startMonth + ' en adelante.'
  );
}

function markSaturdayLunchB_(sheet) {
  var lastRow = sheet.getLastRow();
  if (lastRow < 1) {
    return 0;
  }
  var range = sheet.getRange(1, 1, lastRow, 2);
  var values = range.getDisplayValues();
  var saturday = false;
  var updated = 0;
  for (var i = 0; i < values.length; i++) {
    var hora = String(values[i][0] || '').trim().toUpperCase();
    var nombre = String(values[i][1] || '').trim();
    if (hora === 'DIA') {
      saturday = String(sheet.getRange(i + 1, 3).getDisplayValue() || '').toUpperCase().indexOf('SABADO') >= 0;
      continue;
    }
    if (!saturday) {
      continue;
    }
    if (isTime_(hora, '12:00') || isTime_(hora, '12:20') || isTime_(hora, '12:40')) {
      if (!nombre) {
        values[i][1] = 'ALMUERZO';
        updated++;
      }
    }
    if ((isTime_(hora, '13:00') || isTime_(hora, '13:20') || isTime_(hora, '13:40'))
        && nombre.toUpperCase() === 'ALMUERZO') {
      values[i][1] = '';
      updated++;
    }
  }
  if (updated > 0) {
    range.setValues(values);
  }
  applyAlmuerzoStyleToSheet_(sheet);
  return updated;
}

function applyAlmuerzoStyleToSheet_(sheet) {
  var lastRow = sheet.getLastRow();
  if (lastRow < 1) {
    return;
  }
  var values = sheet.getRange(1, 1, lastRow, 2).getDisplayValues();
  for (var i = 0; i < values.length; i++) {
    var nombre = String(values[i][1] || '').trim().toUpperCase();
    var row = sheet.getRange(i + 1, 1, 1, 6);
    if (nombre === 'ALMUERZO') {
      row.setBackground(ALMUERZO_BACKGROUND);
      row.setFontColor(ALMUERZO_FONT);
      row.setFontWeight('bold');
    } else if (isBookableSlotRow_(values[i][0], values[i][1])) {
      row.setBackground(null);
      row.setFontColor('#000000');
      row.setFontWeight('normal');
    }
  }
}

/**
 * Resalta toda la fila (A-F) según ASISTENCIA. Formato condicional:
 * cambia el color al elegir CONFIRMO / ASISTIO / NO ASISTIO. No borra citas.
 */
function applyAttendanceRowColorsRemainingMonths_() {
  var spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
  var now = new Date();
  var startMonth = now.getFullYear() === YEAR ? now.getMonth() + 1 : 1;
  var processed = [];

  spreadsheet.getSheets().forEach(function (sheet) {
    var match = /^([AB])_(\d{4})_(\d{2})$/.exec(sheet.getName());
    if (!match) {
      return;
    }
    var year = parseInt(match[2], 10);
    var month = parseInt(match[3], 10);
    if (year !== YEAR || month < startMonth) {
      return;
    }
    applyAttendanceRowColorsToSheet_(sheet);
    processed.push(sheet.getName());
  });

  SpreadsheetApp.getUi().alert(
    processed.length
      ? 'Colores de asistencia aplicados en:\n' + processed.join('\n')
      : 'No se encontraron pestañas ' + YEAR + ' del mes ' + startMonth + ' en adelante.'
  );
}

function applyAttendanceRowColorsToSheet_(sheet) {
  var range = sheet.getRange('A:F');
  var kept = sheet.getConditionalFormatRules().filter(function (rule) {
    var condition = rule.getBooleanCondition();
    if (!condition) {
      return true;
    }
    var values = condition.getCriteriaValues();
    var formula = values && values.length ? String(values[0]) : '';
    return formula.indexOf('UPPER(TRIM($F') < 0;
  });

  ASISTENCIA_ROW_COLORS.forEach(function (color) {
    color.values.forEach(function (status) {
      kept.push(
        SpreadsheetApp.newConditionalFormatRule()
          .whenFormulaSatisfied('=UPPER(TRIM($F1))="' + status + '"')
          .setBackground(color.background)
          .setFontColor(color.font)
          .setRanges([range])
          .build()
      );
    });
  });
  sheet.setConditionalFormatRules(kept);
}

function applyAttendanceDropdownToSheet_(sheet) {
  var lastRow = sheet.getLastRow();
  if (lastRow < 1) {
    return;
  }

  sheet.getRange(1, 6, lastRow, 1).clearDataValidations();

  var values = sheet.getRange(1, 1, lastRow, 2).getDisplayValues();
  var rule = SpreadsheetApp.newDataValidation()
    .requireValueInList(ASISTENCIA_OPTIONS, true)
    .setAllowInvalid(false)
    .setHelpText('Elige CONFIRMO, ASISTIO o NO ASISTIO')
    .build();

  var start = null;
  function flush(endExclusive) {
    if (start === null) {
      return;
    }
    sheet.getRange(start, 6, endExclusive - start, 1).setDataValidation(rule);
    start = null;
  }

  for (var i = 0; i < values.length; i++) {
    if (isBookableSlotRow_(values[i][0], values[i][1])) {
      if (start === null) {
        start = i + 1;
      }
    } else {
      flush(i + 1);
    }
  }
  flush(values.length + 1);
}

function isTime_(value, expected) {
  var text = String(value == null ? '' : value).trim();
  if (text === expected) {
    return true;
  }
  return text.indexOf(expected) === 0;
}

function isBookableSlotRow_(hora, nombre) {
  var name = String(nombre == null ? '' : nombre).trim().toUpperCase();
  if (name === 'ALMUERZO') {
    return false;
  }
  var text = String(hora == null ? '' : hora).trim();
  return /^\d{1,2}:\d{2}(:\d{2})?(\s*[AaPp]\.?[Mm]\.?)?$/.test(text);
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
