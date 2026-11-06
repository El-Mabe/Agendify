# Plantilla Meta `cita_recordatorio` (es_CO)

El bot en Railway manda recordatorios con `REMINDER_SEND_MODE=TEMPLATE`, nombre `cita_recordatorio`, idioma `es_CO`. Meta responde **132001** porque esa plantilla no existe.

Debe coincidir con `ReminderPayloadFactory`: 3 variables de cuerpo (nombre, fecha en español, hora 12 h) y 2 botones de respuesta rápida **Confirmar** / **Cancelar**. El payload `rc|…` / `rx|…` lo pone el código al enviar, no al crear la plantilla. El lugar va fijo en el texto de Meta.

## Crear en WhatsApp Manager

1. [Administrador de WhatsApp](https://business.facebook.com/latest/whatsapp_manager/message_templates) de la WABA del **324** (`2222861645311282`).
2. **Crear plantilla**.
3. Categoría: **Utilidad**.
4. Nombre: `cita_recordatorio` (minúsculas, sin espacios).
5. Idioma: **Spanish (COL)** → `es_CO`.
6. Cuerpo (copia tal cual):

```
Hola, {{1}} 😊 Queremos recordarte que tienes cita de optometría con nosotros:

📅 Fecha: {{2}}
🕘 Hora: {{3}}
📍 Lugar: Centro Comercial Petecuy, Local 119, Cra. 9 #15-35, Cali

Por favor, cuéntanos si podrás asistir usando los botones de abajo. ¡Gracias!
```

Ejemplo de variables: `María Pérez` / `sábado 7 de noviembre` / `9:00 a. m.`.

7. Botones → **Respuesta rápida**:
   - Botón 1: `Confirmar`
   - Botón 2: `Cancelar`
8. Enviar a revisión. Estado debe quedar **Approved** / **Activa**.

Sin emojis en el cuerpo: Meta a veces rechaza plantillas de utilidad con emoji.

## Equivalente Graph API

`POST https://graph.facebook.com/v23.0/2222861645311282/message_templates`

Header: `Authorization: Bearer <WHATSAPP_TOKEN>`

```json
{
  "name": "cita_recordatorio",
  "language": "es_CO",
  "category": "UTILITY",
  "components": [
    {
      "type": "BODY",
      "text": "Hola, {{1}} 😊 Queremos recordarte que tienes cita de optometría con nosotros:\n\n📅 Fecha: {{2}}\n🕘 Hora: {{3}}\n📍 Lugar: Centro Comercial Petecuy, Local 119, Cra. 9 #15-35, Cali\n\nPor favor, cuéntanos si podrás asistir usando los botones de abajo. ¡Gracias!",
      "example": {
        "body_text": [["María Pérez", "sábado 7 de noviembre", "9:00 a. m."]]
      }
    },
    {
      "type": "BUTTONS",
      "buttons": [
        { "type": "QUICK_REPLY", "text": "Confirmar" },
        { "type": "QUICK_REPLY", "text": "Cancelar" }
      ]
    }
  ]
}
```

Cuando esté **Approved**, el scheduler (18:00–22:00 Bogotá) podrá enviarla. No hace falta cambiar código ni variables de Railway si el nombre e idioma siguen `cita_recordatorio` / `es_CO`.
