package com.ocnilentes.mediagenda.shared.outbound.google;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;

public class GoogleSheetsApiClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsApiClient.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BASE_DELAY_MS = 2000;

    private final RestClient restClient;
    private final GoogleAccessTokenProvider tokenProvider;
    private final String spreadsheetId;

    public GoogleSheetsApiClient(
            RestClient.Builder restClientBuilder,
            GoogleAccessTokenProvider tokenProvider,
            String spreadsheetId
    ) {
        this.restClient = restClientBuilder
                .baseUrl("https://sheets.googleapis.com/v4")
                .build();
        this.tokenProvider = tokenProvider;
        this.spreadsheetId = spreadsheetId;
    }

    @SuppressWarnings("unchecked")
    public List<List<Object>> getValues(String range) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Map<String, Object> response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/spreadsheets/{spreadsheetId}/values/{range}")
                                .build(spreadsheetId, range))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                        .retrieve()
                        .body(Map.class);

                if (response == null || response.get("values") == null) {
                    return List.of();
                }
                return (List<List<Object>>) response.get("values");
            } catch (RestClientResponseException exception) {
                if (isRateLimited(exception) && attempt < MAX_RETRIES) {
                    sleepBeforeRetry(attempt, range);
                    continue;
                }
                throw new GoogleSheetsException(
                        "Google Sheets getValues failed: " + exception.getResponseBodyAsString(),
                        exception
                );
            }
        }
        return List.of();
    }

    public void appendValues(String range, List<List<Object>> values) {
        try {
            String encodedRange = UriUtils.encodePath(range, StandardCharsets.UTF_8);
            restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/spreadsheets/{spreadsheetId}/values/" + encodedRange + ":append")
                            .queryParam("valueInputOption", "USER_ENTERED")
                            .queryParam("insertDataOption", "INSERT_ROWS")
                            .build(spreadsheetId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("values", values))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw new GoogleSheetsException(
                    "Google Sheets appendValues failed: " + exception.getResponseBodyAsString(),
                    exception
            );
        }
    }

    public void updateValues(String range, List<List<Object>> values) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String encodedRange = UriUtils.encodePath(range, StandardCharsets.UTF_8);
                restClient.put()
                        .uri(uriBuilder -> uriBuilder
                                .path("/spreadsheets/{spreadsheetId}/values/" + encodedRange)
                                .queryParam("valueInputOption", "USER_ENTERED")
                                .build(spreadsheetId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("values", values))
                        .retrieve()
                        .toBodilessEntity();
                return;
            } catch (RestClientResponseException exception) {
                if (isRateLimited(exception) && attempt < MAX_RETRIES) {
                    sleepBeforeRetry(attempt, range);
                    continue;
                }
                throw new GoogleSheetsException(
                        "Google Sheets updateValues failed: " + exception.getResponseBodyAsString(),
                        exception
                );
            }
        }
    }

    private boolean isRateLimited(RestClientResponseException exception) {
        return exception.getStatusCode().value() == 429;
    }

    private void sleepBeforeRetry(int attempt, String range) {
        long delay = RETRY_BASE_DELAY_MS * attempt;
        log.warn("Google Sheets 429 rate limit on range={}, retrying in {}ms (attempt {})", range, delay, attempt);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
