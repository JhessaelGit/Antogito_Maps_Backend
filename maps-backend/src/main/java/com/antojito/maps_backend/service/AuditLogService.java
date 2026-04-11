package com.antojito.maps_backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuditLogService {

    private static final Path LOG_FILE = Path.of("registro.log");

    public void logLogin(String email) {
        writeEvent("LOGIN", "email=" + email);
    }

    public void logLogout(String email) {
        writeEvent("LOGOUT", "email=" + email);
    }

    public void logOwnerRegistry(UUID restaurantUuid, String mail) {
        writeEvent("OWNER_REGISTRY", "restaurantUuid=" + restaurantUuid + " mail=" + mail);
    }

    public void logRestaurantRegistration(UUID uuid, String name) {
        writeEvent(
                "RESTAURANTE_REGISTRO",
                "uuid=" + uuid + " name=" + name);
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
