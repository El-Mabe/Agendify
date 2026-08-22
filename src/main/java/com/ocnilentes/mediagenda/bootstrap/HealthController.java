package com.ocnilentes.mediagenda.bootstrap;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private static final String APPLICATION_NAME = "mediagenda-bot";
    private static final String APPLICATION_VERSION = "1.0.0";

    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("UP", APPLICATION_NAME, APPLICATION_VERSION));
    }

    public record HealthResponse(String status, String application, String version) {
    }
}
