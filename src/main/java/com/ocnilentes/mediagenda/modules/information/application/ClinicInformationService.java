package com.ocnilentes.mediagenda.modules.information.application;

import com.ocnilentes.mediagenda.modules.information.outbound.persistence.ClinicInformationJpaEntity;
import com.ocnilentes.mediagenda.modules.information.outbound.persistence.SpringDataClinicInformationRepository;
import java.util.LinkedHashMap;
import java.util.Map;
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
            Calle 15#8-103, Centro comercial Petecuy, Local 119, Torre A

            📞 Teléfono:
            602 3481840

            🕐 Horario de atención:
            Lunes a Viernes: 8:00 a.m. – 6:00 p.m.
            Sábados: 8:00 a.m. – 5:00 p.m.
            Domingo: cerrado
            Festivos: cerrado
            """.trim();

    private static final Map<String, String> DEFAULT_INFO = Map.of(
            "mensaje", DEFAULT_MESSAGE
    );

    private final SpringDataClinicInformationRepository informationRepository;

    public ClinicInformationService(SpringDataClinicInformationRepository informationRepository) {
        this.informationRepository = informationRepository;
    }

    public String buildInformationMessage() {
        Map<String, String> info = loadInformation();
        String mensaje = info.get("mensaje");
        if (StringUtils.hasText(mensaje)) {
            return mensaje.trim();
        }
        return DEFAULT_MESSAGE;
    }

    private Map<String, String> loadInformation() {
        Map<String, String> info = new LinkedHashMap<>(DEFAULT_INFO);
        for (ClinicInformationJpaEntity entry : informationRepository.findAll()) {
            if (StringUtils.hasText(entry.getKey()) && StringUtils.hasText(entry.getValue())) {
                info.put(entry.getKey().trim().toLowerCase(), entry.getValue().trim());
            }
        }
        return info;
    }
}
