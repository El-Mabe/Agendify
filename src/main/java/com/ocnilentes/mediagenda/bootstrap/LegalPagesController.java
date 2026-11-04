package com.ocnilentes.mediagenda.bootstrap;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LegalPagesController {

    @RequestMapping(
            value = {"/privacy", "/privacy/"},
            method = {RequestMethod.GET, RequestMethod.HEAD},
            produces = MediaType.TEXT_HTML_VALUE)
    public String privacy() {
        return page(
                "Política de privacidad — Ocnilentes",
                """
                <p>Ocni (asistente de WhatsApp de Ocnilentes) trata datos para agendar, cancelar
                y recordar citas de optometría.</p>
                <p><strong>Datos:</strong> número de WhatsApp, nombre, cédula, fecha y hora de la cita,
                y el contenido del chat necesario para atender la solicitud.</p>
                <p><strong>Uso:</strong> prestar el servicio de citas, enviar confirmaciones y recordatorios,
                y operar la agenda de la óptica. No vendemos estos datos.</p>
                <p><strong>Encargados:</strong> Meta (WhatsApp Cloud API), el hosting del asistente y
                la hoja de agenda de la óptica, solo para este fin.</p>
                <p><strong>Conservación:</strong> mientras exista la cita o una obligación legal.
                Ley 1581 de 2012 (Colombia).</p>
                <p>Contacto: escribe <em>menú</em> al WhatsApp de Ocnilentes o acude a la óptica.</p>
                <p><a href="/datadeletion">Cómo eliminar tus datos</a></p>
                """
        );
    }

    @RequestMapping(
            value = {"/terms", "/terms/"},
            method = {RequestMethod.GET, RequestMethod.HEAD},
            produces = MediaType.TEXT_HTML_VALUE)
    public String terms() {
        return page(
                "Términos de servicio — Ocnilentes",
                """
                <p>Ocni es el asistente de WhatsApp de Ocnilentes para agendar, cancelar y
                recordar citas de optometría.</p>
                <p>Al escribirle, aceptas que usemos tu número, nombre, cédula y datos de la cita
                solo para prestar ese servicio.</p>
                <p>No es un canal de urgencias ni de diagnóstico médico.</p>
                <p><a href="/privacy">Política de privacidad</a> ·
                <a href="/datadeletion">Eliminación de datos</a></p>
                """
        );
    }

    @RequestMapping(
            value = "/robots.txt",
            method = {RequestMethod.GET, RequestMethod.HEAD},
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return """
                User-agent: facebookexternalhit
                Allow: /

                User-agent: Facebot
                Allow: /

                User-agent: *
                Allow: /
                """;
    }

    @RequestMapping(
            value = {"/datadeletion", "/datadeletion/", "/eliminar-datos", "/eliminar-datos/",
                    "/data-deletion", "/data-deletion/"},
            method = {RequestMethod.GET, RequestMethod.HEAD},
            produces = MediaType.TEXT_HTML_VALUE)
    public String dataDeletion() {
        return page(
                "Eliminación de datos — Ocnilentes",
                """
                <p>Para eliminar tu número, nombre, cédula y citas asociadas al asistente Ocni:</p>
                <ol>
                  <li>Escribe al WhatsApp de Ocnilentes pidiendo la eliminación de tus datos, o</li>
                  <li>Solicítalo en la óptica (Cali).</li>
                </ol>
                <p>Atenderemos la solicitud en un plazo razonable, salvo datos que debamos conservar
                por ley o por una cita aún vigente.</p>
                <p>Si pediste el borrado desde Facebook, esta página es el seguimiento de esa solicitud.</p>
                <p><a href="/privacy">Política de privacidad</a></p>
                """
        );
    }

    /**
     * Callback oficial de Meta (POST signed_request). Respuesta JSON
     * {url, confirmation_code} según Data Deletion Request Callback.
     */
    @PostMapping(value = {"/datadeletion", "/data-deletion"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> dataDeletionCallback(HttpServletRequest request) {
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String origin = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443
                ? ""
                : ":" + request.getServerPort());
        Map<String, String> body = new LinkedHashMap<>();
        body.put("url", origin + "/datadeletion?id=" + code);
        body.put("confirmation_code", code);
        return body;
    }

    private static String page(String title, String body) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>%s</title>
                </head>
                <body>
                  <h1>%s</h1>
                  %s
                </body>
                </html>
                """.formatted(title, title, body);
    }
}
