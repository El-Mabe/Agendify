"""Genera la plantilla Excel alineada al CONTRATO-AGENDA.md"""
from datetime import date, timedelta
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.datavalidation import DataValidation
from pathlib import Path

wb = Workbook()

header_fill = PatternFill("solid", fgColor="1F4E79")
dia_fill = PatternFill("solid", fgColor="D6EAF8")
lunch_fill = PatternFill("solid", fgColor="F5B7B1")
header_font = Font(color="FFFFFF", bold=True)
bold = Font(bold=True)
thin = Border(
    left=Side(style="thin", color="D0D0D0"),
    right=Side(style="thin", color="D0D0D0"),
    top=Side(style="thin", color="D0D0D0"),
    bottom=Side(style="thin", color="D0D0D0"),
)

DAY_NAMES = {
    0: "LUNES",
    1: "MARTES",
    2: "MIERCOLES",
    3: "JUEVES",
    4: "VIERNES",
    5: "SABADO",
    6: "DOMINGO",
}

COLS = ["HORA", "NOMBRE", "CEDULA", "TELEFONO", "OBSERVACION", "ASISTENCIA"]


def style_header_row(ws, row):
    for col in range(1, 7):
        cell = ws.cell(row, col)
        cell.fill = header_fill
        cell.font = header_font
        cell.alignment = Alignment(horizontal="center")
        cell.border = thin


def time_slots(start_h, start_m, end_h, end_m):
    times = []
    h, m = start_h, start_m
    while (h, m) < (end_h, end_m):
        times.append(f"{h:02d}:{m:02d}")
        m += 20
        if m >= 60:
            h += 1
            m -= 60
    return times


def slots_for(consultorio: str):
    if consultorio == "A":
        morning = time_slots(8, 0, 12, 0)
        lunch = time_slots(12, 0, 13, 0)
        afternoon = time_slots(13, 0, 17, 0)
    else:
        morning = time_slots(9, 0, 13, 0)
        lunch = time_slots(13, 0, 14, 0)
        afternoon = time_slots(14, 0, 18, 0)
    return morning, lunch, afternoon


def add_day_block(ws, start_row, day: date, consultorio: str):
    row = start_row
    # Marker
    ws.cell(row, 1, "DIA")
    ws.cell(row, 2, day.isoformat())
    ws.cell(row, 3, DAY_NAMES[day.weekday()])
    for c in range(1, 4):
        ws.cell(row, c).fill = dia_fill
        ws.cell(row, c).font = bold
    row += 1

    # Headers
    for i, name in enumerate(COLS, start=1):
        ws.cell(row, i, name)
    style_header_row(ws, row)
    row += 1

    morning, lunch, afternoon = slots_for(consultorio)
    first_slot_row = row
    for t in morning + lunch + afternoon:
        ws.cell(row, 1, t)
        ws.cell(row, 1).alignment = Alignment(horizontal="center")
        for c in range(1, 7):
            ws.cell(row, c).border = thin
        if t in lunch:
            ws.cell(row, 2, "ALMUERZO")
            for c in range(1, 7):
                ws.cell(row, c).fill = lunch_fill
        row += 1
    last_slot_row = row - 1

    # blank separator
    row += 1
    return row, first_slot_row, last_slot_row


def build_month_sheet(title: str, consultorio: str, year: int, month: int, sample_days: int = 5):
    ws = wb.create_sheet(title)
    for i, w in enumerate([8, 22, 14, 16, 18, 14], start=1):
        ws.column_dimensions[get_column_letter(i)].width = w

    asistencia_dv = DataValidation(
        type="list",
        formula1='"CONFIRMO,CANCELO,ASISTIO"',
        allow_blank=True,
        showErrorMessage=True,
        errorTitle="ASISTENCIA",
        error="Usa solo: CONFIRMO, CANCELO o ASISTIO",
    )
    ws.add_data_validation(asistencia_dv)

    row = 1
    d = date(year, month, 1)
    added = 0
    while d.month == month and added < sample_days:
        if d.weekday() != 6:  # not Sunday
            row, first_slot, last_slot = add_day_block(ws, row, d, consultorio)
            asistencia_dv.add(f"F{first_slot}:F{last_slot}")
            added += 1
        d += timedelta(days=1)
    ws.freeze_panes = "A1"
    return ws


# --- Instrucciones ---
help_ws = wb.active
help_ws.title = "Instrucciones"
help_ws["A1"] = "MediAgenda — Plantilla según CONTRATO-AGENDA.md"
help_ws["A1"].font = Font(bold=True, size=14, color="1F4E79")
lines = [
    "",
    "Pestañas de este archivo:",
    "  - A_2026_08 / B_2026_08: ejemplo de mes (solo algunos días de muestra).",
    "  - Festivos / Informacion.",
    "",
    "Al iniciar un mes real:",
    "  1) Duplicar A_2026_08 → renombrar A_yyyy_MM (igual para B).",
    "  2) Completar TODOS los días del mes con bloques DIA | yyyy-MM-dd | NOMBRE_DIA.",
    "  3) Actualizar Festivos.",
    "",
    "Contrato completo: docs/CONTRATO-AGENDA.md",
    "",
    "Regla de convivencia:",
    "  Misma celda de hora = misma verdad para bot y recepción.",
    "  Bot escribe NOMBRE + CEDULA + TELEFONO. No toca OBSERVACION al agendar.",
    "  ASISTENCIA (desplegable): CONFIRMO | CANCELO | ASISTIO.",
    "  Bot pone CONFIRMO al confirmar recordatorio. Profesional pone ASISTIO.",
    "  Cancelar (bot) = evento en BD + limpiar NOMBRE/CEDULA/TELEFONO/OBSERVACION/ASISTENCIA.",
]
for i, line in enumerate(lines, start=2):
    help_ws[f"A{i}"] = line
help_ws.column_dimensions["A"].width = 100

# Month samples
build_month_sheet("A_2026_08", "A", 2026, 8, sample_days=5)
build_month_sheet("B_2026_08", "B", 2026, 8, sample_days=5)

# Festivos
fest = wb.create_sheet("Festivos")
fest.append(["fecha"])
fest["A1"].fill = header_fill
fest["A1"].font = header_font
for d in [
    "2026-01-01", "2026-01-12", "2026-03-23", "2026-04-02", "2026-04-03",
    "2026-05-01", "2026-05-18", "2026-06-08", "2026-06-15", "2026-07-20",
    "2026-08-07", "2026-08-17", "2026-10-12", "2026-11-02", "2026-11-16",
    "2026-12-08", "2026-12-25",
]:
    fest.append([d])
fest.column_dimensions["A"].width = 14

# Informacion
info = wb.create_sheet("Informacion")
info.append(["clave", "valor"])
info["A1"].fill = header_fill
info["B1"].fill = header_fill
info["A1"].font = header_font
info["B1"].font = header_font
info.append(["nombre", "OcniLentes"])
info.append(["descripcion", "Atención oftalmológica"])
info.append(["horario", "Consultorio A: 8-12 y 13-17 | Consultorio B: 9-13 y 14-18"])
info.append(["nota", "Para agendar o cancelar, escribe menú."])
info.column_dimensions["A"].width = 14
info.column_dimensions["B"].width = 70

out = Path(r"c:\Github\mediagenda-bot\docs\MediAgenda-Agenda-Plantilla.xlsx")
out.parent.mkdir(parents=True, exist_ok=True)
wb.save(out)
print(out.resolve())
