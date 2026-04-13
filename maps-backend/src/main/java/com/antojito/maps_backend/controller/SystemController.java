package com.antojito.maps_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@Tag(name = "Sistema", description = "Endpoints de estado del backend y conectividad")
public class SystemController {

    private final DataSource dataSource;
    private static final Logger logger = LoggerFactory.getLogger(SystemController.class);

    public SystemController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/app/health")
    @Operation(summary = "Health general", description = "Devuelve estado de disponibilidad del backend")
    @ApiResponse(
            responseCode = "200",
            description = "Backend disponible",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\":\"UP\",\"timestamp\":\"2026-04-10T22:11:24.575Z\"}")))
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/app/health/db")
    @Operation(summary = "Health de base de datos", description = "Verifica conectividad JDBC con la base de datos")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Conexion a base de datos activa",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"status\":\"UP\",\"databaseProduct\":\"PostgreSQL\",\"databaseUrl\":\"jdbc:postgresql://...\",\"timestamp\":\"2026-04-10T22:11:24.575Z\"}"))),
        @ApiResponse(
                responseCode = "503",
                description = "Sin conectividad a base de datos",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"status\":\"DOWN\",\"error\":\"Connection refused\",\"timestamp\":\"2026-04-10T22:11:24.575Z\"}")))
    })
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

    @PostMapping("/log")
    public ResponseEntity<Void> receiveLog(@RequestBody Map<String, String> body) {
        String level = body.get("level");
        String message = body.get("message");
        String email = body.get("email");
        String role = body.get("role");
        String action = body.get("action");
        String timestamp = body.get("timestamp");

        logger.info("[{}] {} | email={} | role={} | action={} | time={}",
                level, message, email, role, action, timestamp);

        return ResponseEntity.ok().build();
    }

    private String sanitizeJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return "unknown";
        }
        return jdbcUrl.replaceAll("(?i)(password=)[^&;]+", "$1***");
    }
}