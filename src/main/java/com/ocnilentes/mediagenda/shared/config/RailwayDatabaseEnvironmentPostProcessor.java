package com.ocnilentes.mediagenda.shared.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Convierte {@code DATABASE_URL} de Railway ({@code postgresql://user:pass@host:port/db})
 * al formato JDBC que espera Spring Boot.
 */
public class RailwayDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DATABASE_URL = "DATABASE_URL";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty(DATABASE_URL);
        if (databaseUrl == null || databaseUrl.isBlank() || databaseUrl.startsWith("jdbc:")) {
            return;
        }

        ParsedPostgresUrl parsed = parsePostgresUrl(databaseUrl);
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url",
                "jdbc:postgresql://" + parsed.host() + ":" + parsed.port() + "/" + parsed.database());
        properties.put("spring.datasource.username", parsed.username());
        properties.put("spring.datasource.password", parsed.password());

        environment.getPropertySources().addFirst(
                new MapPropertySource("railwayDatabaseUrl", properties));
    }

    private static ParsedPostgresUrl parsePostgresUrl(String databaseUrl) {
        String normalized = databaseUrl;
        if (normalized.startsWith("postgres://")) {
            normalized = "postgresql://" + normalized.substring("postgres://".length());
        }
        if (!normalized.startsWith("postgresql://")) {
            throw new IllegalArgumentException("DATABASE_URL debe usar el esquema postgresql://");
        }

        String remainder = normalized.substring("postgresql://".length());
        String userInfo;
        String hostAndDatabase;
        int atIndex = remainder.lastIndexOf('@');
        if (atIndex >= 0) {
            userInfo = remainder.substring(0, atIndex);
            hostAndDatabase = remainder.substring(atIndex + 1);
        } else {
            userInfo = "";
            hostAndDatabase = remainder;
        }

        String[] credentials = userInfo.split(":", 2);
        String username = decode(credentials[0]);
        String password = credentials.length > 1 ? decode(credentials[1]) : "";

        int slashIndex = hostAndDatabase.indexOf('/');
        if (slashIndex < 0) {
            throw new IllegalArgumentException("DATABASE_URL debe incluir el nombre de la base de datos");
        }

        String hostPort = hostAndDatabase.substring(0, slashIndex);
        String database = hostAndDatabase.substring(slashIndex + 1);
        int queryIndex = database.indexOf('?');
        if (queryIndex >= 0) {
            database = database.substring(0, queryIndex);
        }

        String host;
        int port;
        int colonIndex = hostPort.lastIndexOf(':');
        if (colonIndex >= 0) {
            host = hostPort.substring(0, colonIndex);
            port = Integer.parseInt(hostPort.substring(colonIndex + 1));
        } else {
            host = hostPort;
            port = 5432;
        }

        return new ParsedPostgresUrl(host, port, database, username, password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record ParsedPostgresUrl(String host, int port, String database, String username, String password) {
    }
}
