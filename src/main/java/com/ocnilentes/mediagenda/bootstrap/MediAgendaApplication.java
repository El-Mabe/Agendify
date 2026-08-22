package com.ocnilentes.mediagenda.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.ocnilentes.mediagenda")
@ConfigurationPropertiesScan(basePackages = "com.ocnilentes.mediagenda")
@EnableJpaRepositories(basePackages = "com.ocnilentes.mediagenda")
@EntityScan(basePackages = "com.ocnilentes.mediagenda")
@EnableScheduling
public class MediAgendaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediAgendaApplication.class, args);
    }
}
