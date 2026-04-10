package com.antojito.maps_backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuditLogService {

    private static final Path LOG_FILE = Path.of("registro.log");

    public void logLogin(String email) {
        writeEvent("LOGIN", "email=" + email);
    }

    public void logRestaurantRegistration(Long id, String nombre, String correo) {
        writeEvent(
                "RESTAURANTE_REGISTRO",
                "id=" + id + " nombre=" + nombre + " correo=" + correo);
    }

    private void writeEvent(String eventType, String detail) {
        String line = eventType + " | " + detail + " | fecha=" + LocalDateTime.now() + System.lineSeparator();
        try {
            Files.writeString(
                    LOG_FILE,
                    line,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException exception) {
            log.error("No se pudo escribir el registro de auditoria", exception);
        }
    }
}
