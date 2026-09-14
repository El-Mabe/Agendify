# MediAgenda Bot

Asistente de WhatsApp para la gestión de citas de un consultorio.

## Objetivo

Permitir a pacientes interactuar con un consultorio vía WhatsApp para:

- Agendar citas
- Cancelar citas
- Consultar información del consultorio

En el MVP, **Google Sheets** será la fuente de verdad de la agenda.

## Arquitectura

Estructura base preparada para evolucionar a Arquitectura Hexagonal por módulo:

```
com.ocnilentes.mediagenda
├── bootstrap          # Arranque de la aplicación
├── shared
│   ├── config         # Configuration Properties y RestClient
│   ├── exception      # GlobalExceptionHandler
│   ├── response       # Contratos de respuesta compartidos
│   └── util
└── modules
    ├── whatsapp       # Canal WhatsApp / Meta Cloud API
    ├── appointment    # Gestión de citas
    └── information    # Información del consultorio
```

El módulo `whatsapp` ya tiene capas hexagonales porque asumió su primera responsabilidad (enviar mensajes):

```
modules/whatsapp
├── application     # Casos de uso
├── domain          # Puertos y excepciones de dominio
├── inbound/rest    # Controllers
└── outbound/meta   # Adaptador Meta Cloud API
```

## Stack

| Tecnología | Uso |
|------------|-----|
| Java 21 | Lenguaje |
| Spring Boot | Framework |
| Spring MVC | API REST |
| RestClient | Cliente HTTP hacia Meta |
| Lombok | Boilerplate |
| Bean Validation | Validación |
| Actuator | Observabilidad |
| Maven + YAML | Build y configuración |

## Cómo ejecutar

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Perfil local:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Health check:

```bash
curl http://localhost:8080/api/v1/health
```

```json
{
  "status": "UP",
  "application": "mediagenda-bot",
  "version": "1.0.0"
}
```

## Variables de entorno

Perfil `local` (`application-local.yml`):

| Variable | Descripción |
|----------|-------------|
| `WHATSAPP_TOKEN` | Token de WhatsApp Cloud API |
| `WHATSAPP_PHONE_NUMBER_ID` | Phone Number ID de Meta |
| `WHATSAPP_VERIFY_TOKEN` | Token de verificación del webhook (en local: `mediagenda-local-verify`) |
| `GOOGLE_SHEETS_ENABLED` | `true` para usar Sheets; `false` usa memoria |
| `GOOGLE_SHEETS_SPREADSHEET_ID` | ID del spreadsheet de agenda |
| `GOOGLE_SHEETS_CREDENTIALS_PATH` | Ruta al JSON de la service account |

Perfiles disponibles: `local`, `dev`, `prod`.

## Roadmap del MVP

| Sprint | Objetivo |
|--------|----------|
| **1 - Base técnica** | API Spring Boot lista para hablar con Meta |
| **2 - Enviar mensaje** | `POST /api/v1/messages/test` → Meta → WhatsApp |
| **3 - Webhook** | Recibir mensajes entrantes |
| **4 - Motor de conversación** | Menú: Agendar / Cancelar / Información |
| **5 - Google Sheets** | Persistencia de citas en Sheets |

### Sprint 1 (completado)

- [x] Proyecto Spring Boot
- [x] Arquitectura base
- [x] Configuración centralizada
- [x] Endpoint Health
- [x] Cliente HTTP (`RestClient`) listo
- [x] Variables de entorno y perfiles
- [x] GlobalExceptionHandler
- [x] Primer commit

### Sprint 2 (completado)

- [x] Puerto `WhatsappMessageClient`
- [x] Adaptador Meta Cloud API con `RestClient`
- [x] Caso de uso `SendTestMessageService`
- [x] `POST /api/v1/messages/test`

#### Probar envío

Con perfil `local` y variables configuradas:

```powershell
curl -X POST http://localhost:8080/api/v1/messages/test `
  -H "Content-Type: application/json" `
  -d "{\"to\":\"51999999999\",\"message\":\"Hola desde MediAgenda Bot\"}"
```

Respuesta esperada:

```json
{
  "status": "SENT",
  "messageId": "wamid....",
  "to": "51999999999"
}
```

`message` es opcional; si se omite se usa el texto por defecto.

### Sprint 3 (completado)

- [x] Verificación del webhook (`GET`)
- [x] Recepción de notificaciones (`POST`)
- [x] Parseo de mensajes de texto entrantes
- [x] `verify-token` configurado

#### Endpoints

| Método | Ruta | Uso |
|--------|------|-----|
| `GET` | `/api/v1/whatsapp/webhook` | Verificación Meta (`hub.challenge`) |
| `POST` | `/api/v1/whatsapp/webhook` | Mensajes / estados entrantes |

#### Configurar en local

1. Define el mismo token en YAML y en Meta:

```yaml
whatsapp:
  verify-token: mediagenda-local-verify
```

2. Arranca la app con perfil `local`.

3. Expón tu localhost con HTTPS (Meta lo exige), por ejemplo con ngrok:

```powershell
ngrok http 8080
```

4. En Meta → WhatsApp → Configuración → Webhook:
   - **URL de callback:** `https://TU_URL_NGROK/api/v1/whatsapp/webhook`
   - **Verify token:** `mediagenda-local-verify`
   - Suscríbete al campo `messages`

5. Envía un WhatsApp al número de prueba de Meta. En los logs deberías ver:

```text
Incoming WhatsApp message id=... from=5730... text='Hola'
```

### Sprint 4 (completado)

- [x] Motor de conversación
- [x] Menú: Agendar / Cancelar / Información
- [x] Respuesta automática vía WhatsApp

#### Flujo

```text
Usuario: Hola
Bot: menú 1/2/3

Usuario: 1
Bot: pide nombre → lista hasta 5 fechas → lista horas → agenda
     (asignación automática A/B)
```

### Sprint 5 (completado)

- [x] Agenda real con consultorios A y B
- [x] Slots de 20 min y almuerzo cruzado
- [x] Sin citas el mismo día; máx. 5 fechas; sin domingos/festivos
- [x] Asignación al consultorio con menos citas del día
- [x] Cancelar por lista del número (limpia la fila)
- [x] Google Sheets layout visual (`A_yyyy_MM` / `B_yyyy_MM`) según contrato
- [x] Lectura/escritura en celdas de hora (convivencia con recepción)
- [x] Festivos desde pestaña `Festivos`

## Persistencia de citas y sincronización con Google Sheets

PostgreSQL es la fuente operativa de las citas. El bot consulta disponibilidad, citas por
cédula y citas por fecha en la base de datos, por lo que una conversación no necesita leer
las pestañas de agenda de Google Sheets.

Google Sheets se mantiene como interfaz para recepción:

- Las citas creadas, canceladas o confirmadas por el bot se encolan y se reflejan de forma
  asíncrona en Sheets.
- Si Google devuelve un error o un `429`, la operación queda pendiente y se reintenta.
- Los cambios manuales de recepción llegan al backend mediante el Apps Script
  `docs/google-sheets-appointment-sync.gs`.
- Al iniciar y cada 10 minutos se reconcilian únicamente los meses configurados.

Variables de Railway:

```text
GOOGLE_SHEETS_ENABLED=true
GOOGLE_SHEETS_SPREADSHEET_ID_A=<id-archivo-consultorio-A>
GOOGLE_SHEETS_SPREADSHEET_ID_B=<id-archivo-consultorio-B>
GOOGLE_SHEETS_SPREADSHEET_ID_SHARED=<id-opcional-festivos-info>
GOOGLE_SHEETS_CREDENTIALS_JSON=<json-service-account>
APPOINTMENT_SHEET_SYNC_ENABLED=true
APPOINTMENT_SHEET_SYNC_TOKEN=<secreto-largo-y-aleatorio>
APPOINTMENT_SHEET_BOOTSTRAP_ENABLED=false
APPOINTMENT_SHEET_BOOTSTRAP_MONTHS_BACK=1
APPOINTMENT_SHEET_BOOTSTRAP_MONTHS_FORWARD=3
APPOINTMENT_SHEET_RECONCILIATION_ENABLED=false
APPOINTMENT_SHEET_RECONCILIATION_MONTHS_FORWARD=1
APPOINTMENT_SHEET_RECONCILIATION_DELAY_MS=600000
APPOINTMENT_SHEET_RETRY_DELAY_MS=60000
```

Cada consultorio usa su propio Google Spreadsheet. Genera el año con
`docs/google-sheets-year-generator.gs` (`CONSULTORIO='A'` o `'B'`).
Instala `docs/google-sheets-appointment-sync.gs` en **ambos** archivos y configura:

- `MEDIAGENDA_API_URL`
- `SHEET_SYNC_TOKEN`
- `CONSULTORIO=A` o `CONSULTORIO=B`

Comparte los 3 archivos (A, B y shared) con la service account (Editor).

Si aún usas un solo archivo legacy, puedes dejar solo `GOOGLE_SHEETS_SPREADSHEET_ID`.

Después del primer despliegue se puede ejecutar una reconciliación manual:

```bash
curl -X POST \
  "https://tu-servicio.up.railway.app/api/v1/admin/appointments/sheets/import?monthsBack=1&monthsForward=3&reconcileDeletes=false" \
  -H "X-Sheet-Sync-Token: <APPOINTMENT_SHEET_SYNC_TOKEN>"
```

La respuesta separa registros creados, actualizados, sin cambios, cancelados y fallidos.
La importación y la reconciliación destructiva quedan deshabilitadas por defecto: Sheets no
puede cancelar automáticamente una cita válida en PostgreSQL. Mantén habilitado el activador
de edición; activa la reconciliación periódica solo si aceptas incorporar cambios de Sheets.
Festivos e información clínica también se copian a PostgreSQL para evitar lecturas durante
las conversaciones.

#### Flujo de agendar

```text
1 → nombre → elegir fecha → elegir hora → guarda (consultorio automático)
```

#### Google Sheets

Crea un spreadsheet con:

**Agenda** (fila 1 = encabezados):

| id | fecha | hora | consultorio | nombre | telefono | observacion | asistencia |

**Festivos** (desde fila 2, formato `yyyy-MM-dd`):

| fecha |
| 2026-01-01 |
| 2026-05-01 |

**Informacion** (opcional):

| clave | valor |
| nombre | OcniLentes |
| horario | ... |

Luego:

1. Service Account en Google Cloud + Sheets API habilitada + JSON.
2. Comparte el spreadsheet con el email de la service account (Editor).
3. Configura:

```powershell
$env:GOOGLE_SHEETS_ENABLED="true"
$env:GOOGLE_SHEETS_SPREADSHEET_ID="tu-spreadsheet-id"
$env:GOOGLE_SHEETS_CREDENTIALS_PATH="C:\ruta\google-service-account.json"
```

Sin Sheets (`enabled=false`) el flujo corre en memoria.

## Contrato de convivencia (agenda visual)

Para que el bot y la recepción compartan el mismo Google Sheet sin romper la vista de las doctoras, ver:

- [`docs/CONTRATO-AGENDA.md`](docs/CONTRATO-AGENDA.md) — reglas, nombres de pestañas, layout del día, responsabilidades
- [`docs/MediAgenda-Agenda-Plantilla.xlsx`](docs/MediAgenda-Agenda-Plantilla.xlsx) — plantilla `A_yyyy_MM` / `B_yyyy_MM` + Festivos
