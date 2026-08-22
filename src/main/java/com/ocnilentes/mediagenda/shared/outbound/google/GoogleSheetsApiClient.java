package com.ocnilentes.mediagenda.shared.outbound.google;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;

public class GoogleSheetsApiClient {

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
            throw new GoogleSheetsException(
                    "Google Sheets getValues failed: " + exception.getResponseBodyAsString(),
                    exception
            );
        }
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
        } catch (RestClientResponseException exception) {
            throw new GoogleSheetsException(
                    "Google Sheets updateValues failed: " + exception.getResponseBodyAsString(),
                    exception
            );
        }
    }
}
