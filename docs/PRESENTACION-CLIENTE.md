# OcniLentes — Asistente de citas por WhatsApp

**Documento de presentación para el consultorio**  
Versión: agosto 2026  
Audiencia: dirección, recepción y profesionales (no técnico)

---

## 1. ¿Qué es y para qué sirve?

Es un **asistente por WhatsApp** que permite a los pacientes:

- **Agendar** una cita  
- **Cancelar** una cita  
- Consultar **información** del consultorio  
- Recibir un **recordatorio** el día anterior (con opción de confirmar o cancelar)

Al mismo tiempo, **recepción y las profesionales** siguen trabajando en la misma agenda visual de siempre (Google Sheets): lo que escribe el bot aparece en la grilla, y lo que escribe recepción también lo entiende el bot.

**Objetivo:** menos llamadas para agendar, menos confusiones por nombres repetidos, y mejor seguimiento de quién confirmó, quién canceló y quién realmente asistió.

---

## 2. Cómo se ve para el paciente (WhatsApp)

Al escribir, el bot muestra un menú sencillo:

1. Agendar  
2. Cancelar  
3. Información  

### 2.1 Agendar una cita

```text
Paciente elige “Agendar”
        ↓
Se pide la cédula
        ↓
¿Ya está registrado?
   Sí → saluda por su nombre y muestra fechas
   No → pide el nombre completo y lo guarda
        ↓
Elige fecha (hasta 5 opciones disponibles)
        ↓
Elige hora
        ↓
Confirmación de la cita
```

**Detalles importantes:**

- No se agenda para **el mismo día** (desde mañana en adelante).  
- No se ofrecen **domingos** ni **festivos** cargados por recepción.  
- El sistema asigna solo el consultorio (A o B) según disponibilidad; el paciente no elige consultorio.  
- El teléfono se toma del WhatsApp con el que escribe (no hay que pedirlo otra vez).

### 2.2 Cancelar una cita

```text
Paciente elige “Cancelar”
        ↓
Se pide la cédula del paciente
        ↓
Se listan las citas encontradas
        ↓
Elige cuál cancelar
        ↓
Opcional: ¿desea reagendar?
```

**Por qué pedimos cédula y no solo el celular:**  
así una persona puede cancelar desde el teléfono de un familiar o amigo, y no se mezclan pacientes con el mismo nombre.

### 2.3 Información

Muestra los textos del consultorio (horarios, notas, etc.) que recepción mantiene actualizados en la hoja de información.

---

## 3. Recordatorio del día anterior

El día **antes** de la cita (por la tarde/noche), el sistema puede enviar un mensaje de recordatorio.

El paciente puede responder con botones:

| Botón | Qué pasa |
|--------|----------|
| **Confirmar** | Queda registrado que confirmó la cita de mañana |
| **Cancelar** | Se libera el horario y queda registro de quién canceló |

Así recepción ve en la agenda quién ya confirmó, y el paciente llega más preparado.

> **Nota:** para que estos mensajes lleguen siempre (aunque el paciente no haya escrito ese día), en producción se usa una **plantilla de WhatsApp Business** aprobada por Meta. En pruebas se puede usar el modo interactivo cuando hay conversación abierta.

---

## 4. La agenda que ve el equipo (Google Sheets)

Todo el mundo trabaja sobre la **misma hoja**:

| Quién | Qué hace |
|--------|----------|
| **Recepcionista** | Crea el mes, carga festivos, agenda o ajusta citas a mano |
| **Profesional** | Consulta su consultorio y marca si el paciente asistió |
| **Bot** | Escribe y lee en las mismas celdas al agendar, confirmar o cancelar |

### Columnas de cada horario

| Columna | Contenido |
|---------|-----------|
| Hora | Turno de 20 minutos |
| Nombre | Paciente |
| Cédula | Identificación |
| Teléfono | WhatsApp / contacto |
| Observación | Notas libres (el bot no las toca) |
| **Asistencia** | Menú con 3 opciones (ver abajo) |

### Columna Asistencia (menú desplegable)

| Opción | Significado | Quién la usa |
|--------|-------------|--------------|
| *(vacío)* | Cita agendada, sin novedad | Al crear la cita |
| **CONFIRMO** | Confirmó el recordatorio de “mañana” | El bot, cuando el paciente confirma |
| **CANCELO** | Referencia de cancelación (el detalle completo queda en el sistema) | Uso humano opcional |
| **ASISTIO** | La profesional ya atendió al paciente | Profesional / recepción |

**Regla de oro:** solo **ASISTIO** cuenta para el control anual. Confirmar la cita no es lo mismo que haber venido.

---

## 5. Pacientes registrados (base de datos)

Además de la agenda del mes, el sistema guarda un **historial de pacientes** por cédula:

- Nombre  
- Último teléfono usado  
- Última vez que **asistió** (cuando se marca ASISTIO)  
- Fecha estimada del **próximo control** (hoy: **11 meses** después de la asistencia)

Esto permite, más adelante, avisar automáticamente cuando se acerque el control anual.

### Cancelaciones: qué se guarda

Cuando alguien cancela por WhatsApp, el sistema registra:

- Qué cita era (fecha y hora)  
- Cédula del paciente  
- **Qué número de WhatsApp** hizo la cancelación  

Así, aunque el horario se libere en la hoja (para poder agendar a otra persona), no se pierde la trazabilidad.

---

## 6. Cierre del día (asistencia → control)

Entre **8:00 p. m. y 10:00 p. m.** (hora de Bogotá), el sistema revisa las citas del día (y del día anterior, por si marcaron tarde) y busca filas con **ASISTIO**.

Para cada una:

1. Actualiza la fecha de última asistencia del paciente  
2. Calcula la próxima revisión a **11 meses**  
3. Deja constancia en el historial  

**Responsabilidad del equipo:** marcar **ASISTIO** cuando el paciente fue atendido. Sin esa marca, el control anual no avanza.

---

## 7. Control anual (diseño acordado)

Meta de negocio: recordar al paciente cerca del año de su última atención, para que vuelva a agendar.

| Momento | Acción acordada |
|---------|-----------------|
| A los **11 meses** de la asistencia | Primer aviso: “se acerca tu control; ¿agendamos?” |
| Si no agenda | Un segundo aviso **una semana antes** de cumplirse el año |
| Si aún no agenda | **No insistir más** (evitar molestia / quejas) |
| Si **ya tiene cita futura** | **No enviar** el aviso de control |

**Estado actual:**

- Ya está la base: pacientes, fecha de asistencia y próxima revisión.  
- El envío automático de esos mensajes de control por WhatsApp es el **siguiente paso** a implementar.

---

## 8. Mapa general del sistema (visión simple)

```text
                    WhatsApp del paciente
                            │
                            ▼
                    Asistente OcniLentes
                     /              \
                    /                \
           Agenda Google Sheets    Historial de pacientes
           (día a día, horarios)   (cédula, asistencias,
            recepción + doctora)    cancelaciones, próximo control)
```

- **Sheets** = operación diaria (quién viene hoy / mañana).  
- **Historial** = memoria a largo plazo (quién es, cuándo vino, cuándo toca volver).

---

## 9. Beneficios para el consultorio

1. **Menos carga en recepción** para agendar y cancelar rutinas.  
2. **Menos errores por homónimos** gracias a la cédula.  
3. **Cancelar desde otro celular** sin perder la cita correcta.  
4. **Recordatorio** con confirmación visible en la agenda.  
5. **Misma hoja** para bot y humanos: no hay “dos agendas”.  
6. Base lista para el **control a 11 meses** sin depender solo de la memoria del equipo.

---

## 10. Qué necesita el equipo para que funcione bien

| Rol | Acción clave |
|-----|----------------|
| Recepción | Preparar cada mes (pestañas A y B), mantener festivos e información |
| Recepción / profesional | Usar el desplegable de asistencia; marcar **ASISTIO** al atender |
| Dirección | Definir textos del recordatorio y, cuando toque, la plantilla oficial de WhatsApp |
| Paciente | Usar el menú del bot; confirmar o cancelar con los botones del recordatorio |

---

## 11. Resumen de flujos (una página)

| Flujo | Entrada | Resultado |
|-------|---------|-----------|
| Agendar | WhatsApp → cédula → (nombre si es nuevo) → fecha → hora | Cita en la hoja + paciente en historial |
| Cancelar | WhatsApp → cédula → elegir cita | Horario libre + registro de cancelación |
| Recordatorio día antes | Automático | Mensaje; Confirmar o Cancelar |
| Confirmar | Botón Confirmar | Asistencia = CONFIRMO en la hoja |
| Atender | Profesional marca ASISTIO | Historial: última visita + próximo control (+11 meses) |
| Control anual | (próxima fase) | Avisos a 11 meses y 1 semana antes; sin insistir si no agenda |

---

## 12. Próximos pasos sugeridos (para alinear con el cliente)

1. Probar en el día a día: agendar, cancelar, marcar ASISTIO.  
2. Activar recordatorio del día anterior con plantilla oficial de WhatsApp.  
3. Implementar los mensajes de **control a 11 meses** (con botón para agendar de nuevo).  
4. Capacitación corta a recepción sobre el desplegable de asistencia.

---

*Documento elaborado para presentación del asistente de citas OcniLentes / MediAgenda.  
Si desea una versión en PDF o diapositivas a partir de este texto, se puede generar en el siguiente paso.*
