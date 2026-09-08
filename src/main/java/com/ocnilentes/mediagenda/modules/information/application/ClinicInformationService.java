package com.ocnilentes.mediagenda.modules.information.application;

import com.ocnilentes.mediagenda.shared.config.GoogleSheetsProperties;
import com.ocnilentes.mediagenda.shared.outbound.google.GoogleSheetsApiClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ClinicInformationService {

    private static final String DEFAULT_MESSAGE = """
            Somos una óptica en Cali con más de 20 años de experiencia en el mercado 👓

            📋 Nuestros servicios:
            - Citas de optometría
            - Citas de ortóptica
            - Venta de lentes y monturas
            - Gafas de sol

            📍 Ubicación:
            Centro Comercial Petecuy, Local 119
            Cra. 9 #15-35, Cali

            📞 Teléfono:
            (2) 888-1500

            🕐 Horario de atención:
            Lunes a Viernes: 8:00 a.m. – 6:00 p.m.
            Sábados: 8:00 a.m. – 5:00 p.m.
            Domingo: cerrado
            Festivos: cerrado

            Escribe *menú* para volver a las opciones. 💙
            """.trim();

    private static final Map<String, String> DEFAULT_INFO = Map.of(
            "mensaje", DEFAULT_MESSAGE
    );
    private static final long INFO_CACHE_TTL_MS = java.time.Duration.ofMinutes(10).toMillis();

    private final ObjectProvider<GoogleSheetsApiClient> sheetsApiClientProvider;
    private final GoogleSheetsProperties properties;
    private volatile Map<String, String> cachedInfo;
    private volatile long infoCacheExpiresAtMs;

    public ClinicInformationService(
            ObjectProvider<GoogleSheetsApiClient> sheetsApiClientProvider,
            GoogleSheetsProperties properties
    ) {
        this.sheetsApiClientProvider = sheetsApiClientProvider;
        this.properties = properties;
    }

    public String buildInformationMessage() {
        Map<String, String> info = loadInformationCached();
        String mensaje = info.get("mensaje");
        if (StringUtils.hasText(mensaje)) {
            return mensaje.trim();
        }
        return DEFAULT_MESSAGE;
    }

    private Map<String, String> loadInformationCached() {
        long now = System.currentTimeMillis();
        if (cachedInfo != null && now < infoCacheExpiresAtMs) {
            return cachedInfo;
        }
        Map<String, String> info = loadInformation();
        cachedInfo = info;
        infoCacheExpiresAtMs = now + INFO_CACHE_TTL_MS;
        return info;
    }

    private Map<String, String> loadInformation() {
        GoogleSheetsApiClient client = sheetsApiClientProvider.getIfAvailable();
        if (client == null || !properties.enabled()) {
            return DEFAULT_INFO;
        }

        try {
            String sheet = StringUtils.hasText(properties.informationSheet())
                    ? properties.informationSheet()
                    : "Informacion";
            List<List<Object>> values = client.getValues(sheet + "!A2:B");
            Map<String, String> info = new LinkedHashMap<>(DEFAULT_INFO);

            for (List<Object> row : values) {
                if (row == null || row.size() < 2) {
                    continue;
                }
                String key = Optional.ofNullable(row.get(0)).map(Object::toString).orElse("").trim().toLowerCase();
                String value = Optional.ofNullable(row.get(1)).map(Object::toString).orElse("").trim();
                if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                    info.put(key, value);
                }
            }
            return info;
        } catch (Exception exception) {
            return DEFAULT_INFO;
        }
    }
}
