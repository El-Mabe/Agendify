package com.ocnilentes.mediagenda.shared.outbound.google;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.ocnilentes.mediagenda.shared.config.GoogleSheetsProperties;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.util.StringUtils;

public class GoogleAccessTokenProvider {

    private static final String SHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets";

    private final GoogleCredentials credentials;

    public GoogleAccessTokenProvider(GoogleSheetsProperties properties) {
        this.credentials = loadCredentials(properties.credentialsPath()).createScoped(List.of(SHEETS_SCOPE));
    }

    public String getAccessToken() {
        try {
            credentials.refreshIfExpired();
            if (credentials.getAccessToken() == null || !StringUtils.hasText(credentials.getAccessToken().getTokenValue())) {
                throw new GoogleSheetsException("Google access token is empty");
            }
            return credentials.getAccessToken().getTokenValue();
        } catch (IOException exception) {
            throw new GoogleSheetsException("Failed to refresh Google access token", exception);
        }
    }

    private GoogleCredentials loadCredentials(String credentialsPath) {
        if (!StringUtils.hasText(credentialsPath)) {
            throw new GoogleSheetsException("google.sheets.credentials-path is not configured");
        }

        Path path = Path.of(credentialsPath);
        if (!Files.exists(path)) {
            throw new GoogleSheetsException("Google credentials file not found: " + credentialsPath);
        }

        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            return ServiceAccountCredentials.fromStream(inputStream);
        } catch (IOException exception) {
            throw new GoogleSheetsException("Failed to load Google service account credentials", exception);
        }
    }
}
