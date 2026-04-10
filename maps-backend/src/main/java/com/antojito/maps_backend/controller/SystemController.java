package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.ApiMessageResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemController {

    private final DataSource dataSource;

    public SystemController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/")
    public ResponseEntity<ApiMessageResponse> root() {
        return ResponseEntity.ok(new ApiMessageResponse("Antojitos Maps Backend is running"));
    }

    @GetMapping({"/api/v1/health", "/api/health"})
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(body);
    }

    @GetMapping({"/api/v1/health/db", "/api/health/db"})
    public ResponseEntity<Map<String, Object>> databaseHealth() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());

        try (Connection connection = dataSource.getConnection()) {
            body.put("status", "UP");
            body.put("databaseProduct", connection.getMetaData().getDatabaseProductName());
            body.put("databaseUrl", sanitizeJdbcUrl(connection.getMetaData().getURL()));
            return ResponseEntity.ok(body);
        } catch (SQLException exception) {
            body.put("status", "DOWN");
            body.put("error", exception.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }

    private String sanitizeJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return "unknown";
        }
        return jdbcUrl.replaceAll("(?i)(password=)[^&;]+", "$1***");
    }
}
