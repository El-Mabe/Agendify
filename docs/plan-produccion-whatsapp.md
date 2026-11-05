# Inventario y plan — Ocni / Agendify

Fecha: 2026-11-04. Audiencia: técnica. No es un tutorial genérico de WhatsApp.

## Criterio de hecho

Cualquier número de WhatsApp escribe **Hola** al **324** (ambiente de pruebas) y Ocni responde. En Railway debe aparecer `Incoming WhatsApp message from=57…` y Meta `sent`/`delivered`.

El **315** no es el ambiente actual: queda **Pendiente** hasta que WhatsApp lo apruebe. Entonces pasará a ser el número de **producción**. El 324 sigue como pruebas.

## Qué ya está hecho (comprobado)

| Pieza | Estado | Evidencia |
|---|---|---|
| Bot Spring Boot (`mediagenda-bot`) en Railway | En línea | Servicio Agendify, `agendify-production-7274.up.railway.app` |
| Postgres en el mismo proyecto Railway | Online | Canvas `supportive-fascination` |
| Webhook | Código + URL | `GET/POST /api/v1/whatsapp/webhook` |
| Envío saliente Graph API | Funciona | POST de prueba a Railway → reply → Meta `delivered` |
| Conversación Ocni | En código | Saludo «Hola, soy Ocni…», menú, listas de fechas, botones mañana/tarde |
| Sheets + recordatorios | En código / Railway | Commits `4adb25a` y posteriores |
| Resolver LID vs `wa_id` | En código | Evita contestar al ID interno de Meta |
| Páginas legales **en Railway** | HTTP 200 | `/privacy`, `/terms`, `/datadeletion`, `/robots.txt` |
| Under Attack Mode Railway | **Apagado** | Settings → Edge → botón Activate (no Deactivate) |
| CDN / Edge Rules | Apagado / vacío | Mismo panel Edge |

## Bloqueo actual (comprobado)

La app Meta **OcniLentes** (`914742560998242`) está **sin publicar**.

Meta **no entrega webhooks reales** de producción a apps en Development (documentación WhatsApp: *some webhooks will not be sent if your app is in Dev mode*). Por eso:

- El Test del dashboard y un POST inyectado a Railway **sí** llegan.
- Un `Hola` real desde 318 (u otro número sin rol) al 324 **no** aparece en logs.

Publicar exige Configuración básica: política, términos, eliminación de datos, categoría, icono, etc.

El campo de eliminación en Meta sigue en rojo. El Sharing Debugger marca **403** sobre las URLs de Railway (*robots.txt block*). Railway responde **200** a `facebookexternalhit` desde IPs normales; el crawler de Meta **no llega al contenedor**. Under Attack Mode **no** es la causa.

**Conclusión:** no uses `*.up.railway.app` como URL legal en Meta. Usa GitHub Pages (este cambio).

## Números y WABA

| Rol | Número | WABA | Phone number ID | Estado |
|---|---|---|---|---|
| Pruebas (ahora) | `+57 324 7273774` | `2222861645311282` | `1379904421872907` | Cloud API conectado |
| Producción (después) | `+57 315 …` | `3999691243661096` | (cuando Meta lo asigne) | Pendiente |

Negocio: `2116905558857355`. No re-registrar WhatsApp en el 324. No forzar el 315 mientras esté Pendiente.

Cuando el 315 pase:

1. En Railway, `WHATSAPP_PHONE_NUMBER_ID` (y token si el WABA es otro).
2. Mismo webhook: `https://agendify-production-7274.up.railway.app/api/v1/whatsapp/webhook`.
3. Verificar el webhook en esa WABA.
4. Probar Hola al 315 desde un número cualquiera.
5. El 324 puede seguir como pruebas con su propio phone_number_id (haría falta un segundo servicio o perfil; hoy hay **un** `WHATSAPP_PHONE_NUMBER_ID`).

## Arquitectura (repo)

```
Railway Agendify  →  POST /api/v1/whatsapp/webhook
                  →  ConversationService (Ocni)
                  →  Graph API (WHATSAPP_TOKEN + PHONE_NUMBER_ID)
                  →  Postgres + Google Sheets
```

Variables (valores en Railway, no en git): `WHATSAPP_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_VERIFY_TOKEN`, Postgres, Sheets.

## Qué falta (orden)

### 1. GitHub Pages (legal para Meta)

Tras el push de este commit, en GitHub:

1. Repo **El-Mabe/Agendify** → Settings → Pages.
2. Source: **Deploy from a branch**.
3. Branch: `main`, folder: `/docs`. Save.
4. Si Pages no deja publicar un repo **privado**, o haces el repo público (estas HTML no tienen secretos) o usas GitHub Pro.

URLs esperadas (proyecto Pages):

- Privacidad: `https://el-mabe.github.io/Agendify/privacy.html`
- Términos: `https://el-mabe.github.io/Agendify/terms.html`
- Eliminación: `https://el-mabe.github.io/Agendify/datadeletion.html`

Comprueba en el navegador que den **200** (no 404).

### 2. Depurador de Meta

[Sharing Debugger](https://developers.facebook.com/tools/debug/) → cada URL de Pages → Depurar → Volver a extraer hasta **código 200** (no 403).

### 3. Configuración básica OcniLentes

Pegar **tal cual** (desplegable de eliminación = *URL de las instrucciones*, no callback):

- Política: `https://el-mabe.github.io/Agendify/privacy.html`
- Términos: `https://el-mabe.github.io/Agendify/terms.html`
- Eliminación: `https://el-mabe.github.io/Agendify/datadeletion.html`
- Dominios de la app: `el-mabe.github.io`
- Categoría: ya tienes *Bots de Messenger para empresas* (válida) o Negocios.
- Icono: JPG/PNG ≤ 5 MB (sigue vacío; Meta lo marca requerido para Live).

Guardar cambios. El recuadro de eliminación no debe quedar rojo.

### 4. Publicar / En vivo

App Dashboard → Publicar / modo Live. Si pide requisitos: icono, categoría, email de contacto, términos.

### 5. Prueba en el 324

Desde un número **sin rol** en la app (p. ej. 318):

1. WhatsApp al **+57 324 7273774**: `Hola`.
2. Railway logs (sin filtro de un solo número): `Incoming WhatsApp message from=57318…`
3. Respuesta Ocni en el teléfono.

Si no hay log: la app sigue en Dev o el webhook de esa WABA no apunta a Railway.

### 6. Más adelante — 315 producción

Esperar estado distinto de Pendiente. Recablear `WHATSAPP_PHONE_NUMBER_ID` / token. Misma URL de webhook. Repetir el Hola contra el 315.

## Qué no hace falta tocar ahora

- Reescribir el motor de conversación.
- Volver a dar de alta Cloud API en el 324.
- Activar Under Attack Mode.
- Usar `/eliminar-datos` o `/datadeletion` de Railway **en el formulario de Meta**.
- Callback JSON de eliminación (POST) para publicar: Meta acepta la URL de instrucciones HTML.

## Referencias oficiales

- [Basic Settings](https://developers.facebook.com/documentation/development/create-an-app/app-dashboard/basic-settings) — la URL de eliminación puede ser una sección de la política.
- [WhatsApp webhooks](https://developers.facebook.com/documentation/business-messaging/whatsapp/webhooks/overview) — Live mode.
- [Data deletion callback](https://developers.facebook.com/documentation/development/create-an-app/app-dashboard/data-deletion-callback) — opcional si ya hay instrucciones.
