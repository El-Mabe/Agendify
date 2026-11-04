package com.ocnilentes.mediagenda.bootstrap;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LegalPagesController {

    @GetMapping(value = "/privacy", produces = MediaType.TEXT_HTML_VALUE)
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
                <p><a href="/eliminar-datos">Cómo eliminar tus datos</a></p>
                """
        );
    }

    @GetMapping(value = "/eliminar-datos", produces = MediaType.TEXT_HTML_VALUE)
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
                <p><a href="/privacy">Política de privacidad</a></p>
                """
        );
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
