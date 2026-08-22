# Contrato de Agenda — MediAgenda Bot

Documento de acuerdo entre **bot**, **recepcionista** y **doctoras**.  
Objetivo: convivir en el mismo Google Spreadsheet sin pelear formatos.

## 1. Principio

La **grilla visual** (hora → paciente) es la fuente de verdad que ve el humano.  
El bot lee y escribe **en las mismas celdas**, con reglas fijas.

```text
Recepcionista / Doctoras     Bot WhatsApp
         \                     /
          \                   /
           ▼                 ▼
        Misma pestaña A_yyyy_MM o B_yyyy_MM
        Misma fila de hora del día
```

- Celda de paciente vacía → horario libre  
- Celda con nombre (y/o teléfono) → horario ocupado  
Da igual quién lo escribió.

---

## 2. Estructura del Spreadsheet

| Pestaña | Quién la usa | Propósito |
|---------|--------------|-----------|
| `A_yyyy_MM` | Doctora A, recepción, bot | Agenda visual consultorio A del mes |
| `B_yyyy_MM` | Doctora B, recepción, bot | Agenda visual consultorio B del mes |
| `Festivos` | Recepción | Fechas no bookables (`yyyy-MM-dd`) |
| `Informacion` | Bot (opción 3) | Textos del consultorio |
| `Instrucciones` | Todos | Este contrato resumido |

Ejemplos de nombres válidos:

- `A_2026_08`
- `B_2026_08`
- `A_2026_09`

**No renombrar** con otros formatos (`Agosto A`, `A agosto`, etc.). El bot no los encontrará.

---

## 3. Layout dentro de `A_yyyy_MM` / `B_yyyy_MM`

Los días van **apilados de arriba hacia abajo**.  
Cada día es un bloque con esta forma exacta:

### 3.1 Fila marcadora del día (obligatoria)

| Columna A | Columna B | Columna C |
|-----------|-----------|-----------|
| `DIA` | `2026-08-05` | `MIERCOLES` |

- `A` = texto fijo `DIA` (así el bot detecta el bloque)  
- `B` = fecha ISO `yyyy-MM-dd`  
- `C` = nombre del día (solo lectura humana; el bot no lo exige)

### 3.2 Fila de encabezados (justo debajo)

| A | B | C | D | E | F |
|---|---|---|---|---|---|
| HORA | NOMBRE | CEDULA | TELEFONO | OBSERVACION | ASISTENCIA |

### 3.3 Filas de horarios (20 minutos)

Según consultorio:

**Consultorio A**

| Tramo | Horas (inicio de slot) |
|-------|-------------------------|
| Mañana | 08:00 … 11:40 |
| Almuerzo | 12:00, 12:20, 12:40 → dejar NOMBRE vacío o `ALMUERZO` (no bookable) |
| Tarde | 13:00 … 16:40 |

**Consultorio B**

| Tramo | Horas (inicio de slot) |
|-------|-------------------------|
| Mañana | 09:00 … 12:40 |
| Almuerzo | 13:00, 13:20, 13:40 → no bookable |
| Tarde | 14:00 … 17:40 |

Formato de hora en columna A: `HH:mm` (ej. `08:00`, `10:20`).

### 3.4 Separador

Una fila en blanco entre el fin de un día y el `DIA` del siguiente.

### 3.5 Columna ASISTENCIA (obligatoria para control)

Validación de datos (lista / desplegable) en columna **F**:

| Valor | Quién lo escribe | Significado |
|-------|------------------|-------------|
| *(vacío)* | Default al agendar | Cita agendada sin novedad |
| `CONFIRMO` | Bot (botón Confirmar del recordatorio) | Paciente confirmó la cita de mañana |
| `CANCELO` | Uso humano opcional / histórico | Preferir auditoría en BD al cancelar por bot |
| `ASISTIO` | Profesional / recepción | Paciente atendido → base del control a 11 meses |

- Normalizar sin tildes y en mayúsculas exactamente como arriba.  
- Al cancelar por el bot: se guarda evento `CANCELLED` en Postgres (cédula + teléfono que canceló) y se **libera el slot** (limpia B–F).  
- Job nocturno (20–22 h) solo procesa `ASISTIO`.

---

## 4. Cómo decide el bot dónde escribir

Al agendar una cita concreta:

```text
fecha       = 2026-08-05
hora        = 10:20
consultorio = B   (regla: menos citas ese día entre los libres)
```

Pasos del bot:

1. Abrir pestaña `B_2026_08`
2. Buscar fila donde `A=DIA` y `B=2026-08-05`
3. Bajo ese bloque, buscar fila donde `A=10:20`
4. Si `NOMBRE` y `TELEFONO` están vacíos → escribir  
5. Si ya hay datos → horario ocupado (ofrecer otro)

Al cancelar:

1. Pedir cédula al usuario
2. Buscar en A y B (rango de meses) filas con esa `CEDULA`
3. Mostrar lista al paciente
4. Limpiar `NOMBRE`, `CEDULA`, `TELEFONO`, `OBSERVACION`, `ASISTENCIA` de esa fila  
   (no borrar la fila de la hora)

---

## 5. Reglas de negocio (bot)

| Regla | Valor |
|-------|--------|
| Misma día | No se agenda (desde mañana) |
| Días mostrados | Hasta 5 disponibles |
| Excluir | Domingos + fechas en `Festivos` |
| Slot | 20 minutos |
| Almuerzo | Cruzado (si A almuerza, B puede atender y viceversa) |
| Asignación A/B | Automática: el de **menos citas ese día** entre los que tengan esa hora libre |
| Cédula por WhatsApp | **Sí (obligatoria)** — primero se pide cédula; si el paciente ya está en BD se omite el nombre |
| Cancelación | Por **cédula** (no por teléfono del chat) |
| Pacientes | Tabla `patients` en PostgreSQL (`cedula`, `full_name`, `last_phone`, `last_attended_at`, `next_follow_up_on`) |
| Recordatorios | 1 aviso el **día anterior** (18:00–22:00 Bogotá) con botones Confirmar/Cancelar |
| ASISTENCIA (desplegable) | Solo: `CONFIRMO` \| `CANCELO` \| `ASISTIO` |
| Sync asistencia | Job 20:00–22:00 Bogotá: filas con `ASISTIO` → actualiza control a 11 meses |
| OBSERVACION | La deja vacía el bot |

---

## 6. Responsabilidades

### Recepcionista

1. **Al iniciar un mes:** duplicar plantilla → crear `A_yyyy_MM` y `B_yyyy_MM` con todos los días del mes (bloques `DIA`).
2. Mantener `Festivos` actualizado.
3. Agendar / cancelar / mover citas **en la grilla** (mismo sitio que el bot).
4. No cambiar nombres de pestañas ni columnas.
5. No insertar columnas en medio del bloque.
6. Si crea un día suelto, debe incluir la fila `DIA | yyyy-MM-dd | ...`.

### Doctoras

1. Consultar su pestaña del mes (`A_…` o `B_…`).
2. Usar `OBSERVACION` / `ASISTENCIA` en consulta (el bot no las toca al agendar).

### Bot

1. Leer/escribir solo según este contrato.
2. No crear pestañas de mes (si faltan, avisa “mes no preparado”).
3. No modificar almuerzos ni estructura de horas.

### Desarrollo / API

1. Configurar ID del spreadsheet + credenciales.
2. Implementar el parser del contrato (buscar `DIA`, hora, columnas).
3. No asumir layouts distintos sin actualizar este documento.

---

## 7. Checklist mensual (recepción) — ~5 minutos

- [ ] Crear `A_yyyy_MM` y `B_yyyy_MM` desde la plantilla
- [ ] Verificar que cada día tenga fila `DIA` + fecha ISO
- [ ] Cargar/revisar festivos del mes en `Festivos`
- [ ] Compartir el spreadsheet con la service account (Editor), si es archivo nuevo

---

## 8. Convivencia ante conflictos

| Situación | Qué pasa |
|-----------|----------|
| Recepción ocupa 10:20 y luego el bot intenta 10:20 | Bot ve ocupado y ofrece otra hora |
| Bot agenda y recepción mira la hoja | Ve el nombre/teléfono en esa hora |
| Ambos escriben el mismo slot a la vez | Gana el último write; el bot debe re-leer antes de confirmar (MVP: reintento / mensaje de error) |
| Falta pestaña del mes | Bot: “Agenda del mes no disponible; contacta al consultorio” |

---

## 9. Configuración técnica (app)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mediagenda
    username: mediagenda
    password: mediagenda

google:
  sheets:
    enabled: true
    spreadsheet-id: "<ID>"
    credentials-path: "<ruta-json>"
    # convención de pestañas:
    #   {consultorio}_{yyyy}_{MM}  →  A_2026_08
```

Postgres local (Docker):

```bash
docker compose up -d
```

Recordatorios:

```yaml
reminder:
  enabled: true
  day-before-hour: 18
  window-end-hour: 22
  send-mode: INTERACTIVE   # o TEMPLATE con plantilla Meta aprobada
  template-name: cita_recordatorio
  template-language: es_CO
```

Ejecución manual (pruebas): `POST /api/v1/internal/reminders/run?force=true`

Sync de asistencia (`ASISTIO` → control 11 meses):

```yaml
attendance-sync:
  enabled: true
  window-start-hour: 20
  window-end-hour: 22
```

`POST /api/v1/internal/attendance/sync?force=true`

Plantilla Excel de referencia:

`docs/MediAgenda-Agenda-Plantilla.xlsx`

---

## 10. Evolución futura (opcional)

Si más adelante la grilla se vuelve pesada para el bot:

1. Se agrega pestaña oculta `Agenda` (tabla plana).
2. Apps Script sincroniza grilla ↔ `Agenda`.
3. El bot pasa a usar solo `Agenda`.

**No es necesario para el MVP** si se respeta este contrato.
