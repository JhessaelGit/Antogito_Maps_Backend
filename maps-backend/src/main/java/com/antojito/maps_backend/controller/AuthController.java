package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.ApiMessageResponse;
import com.antojito.maps_backend.dto.ClientLoginRequest;
import com.antojito.maps_backend.dto.RestaurantLoginResponse;
import com.antojito.maps_backend.dto.RestaurantLogoutRequest;
import com.antojito.maps_backend.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/restaurant")
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
@RequiredArgsConstructor
@Tag(name = "Restaurant Auth", description = "Autenticacion de owners de restaurantes (Firebase gestionado por el backend)")
public class AuthController {

    private final AuditLogService auditLogService;
    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${firebase.api-key}")
    private String firebaseApiKey;

    // ─────────────────────────────────────────────────────────────
    // REGISTRY: crea owner en Firebase + guarda en BD
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/registry")
    @Operation(summary = "Registrar owner", description = "Crea el usuario owner en Firebase y lo registra en la BD. Acepta email y password directamente.")
    public ResponseEntity<ApiMessageResponse> registry(@Valid @RequestBody ClientLoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // 1. Verificar que no exista
        try {
            jdbcTemplate.queryForObject("select uuid from owner_account where mail = ?", UUID.class, email);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un owner con ese mail");
        } catch (EmptyResultDataAccessException e) {
            // No existe => continuar
        }

        createFirebaseUser(email, request.getPassword());

        // 3. Insertar en BD
        try {
            jdbcTemplate.update(
                    "insert into owner_account (uuid, mail, password) values (?, ?, ?)",
                    UUID.randomUUID(), email, "FIREBASE_MANAGED");
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un owner con ese mail");
        }

        auditLogService.logOwnerRegistration(email);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiMessageResponse("owner registrado"));
    }

    // ─────────────────────────────────────────────────────────────
    // LOGIN: autentica con Firebase REST + devuelve datos del owner
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/login")
    @Operation(summary = "Login de owner", description = "Autentica email y password contra Firebase y devuelve datos del owner con sus restaurantes.")
    public ResponseEntity<RestaurantLoginResponse> login(@Valid @RequestBody ClientLoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // 1. Autenticar contra Firebase REST
        String firebaseUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + firebaseApiKey;
        try {
            restTemplate.postForEntity(firebaseUrl,
                    Map.of("email", email, "password", request.getPassword(), "returnSecureToken", true),
                    Map.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contrasena incorrectos");
        }

        // 2. Buscar owner en BD
        UUID ownerUuid;
        try {
            ownerUuid = jdbcTemplate.queryForObject(
                    "select uuid from owner_account where mail = ?", UUID.class, email);
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El owner no esta registrado en el sistema");
        }

        // 3. Obtener restaurantes del owner
        List<UUID> restaurantIds = jdbcTemplate.queryForList(
                "select id_restaurant from owner_restaurant where id_owner = ?", UUID.class, ownerUuid);

        auditLogService.logLogin(email);
        return ResponseEntity.ok(new RestaurantLoginResponse(ownerUuid, email, restaurantIds, "login correcto"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout de owner")
    public ResponseEntity<ApiMessageResponse> logout(@Valid @RequestBody RestaurantLogoutRequest request) {
        auditLogService.logLogout(request.getMail());
        return ResponseEntity.ok(new ApiMessageResponse("logout registrado"));
    }

    private void createFirebaseUser(String email, String password) {
        String firebaseUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + firebaseApiKey;
        try {
            restTemplate.postForEntity(
                    firebaseUrl,
                    Map.of("email", email, "password", password, "returnSecureToken", true),
                    Map.class);
        } catch (HttpClientErrorException exception) {
            String responseBody = exception.getResponseBodyAsString();
            if (responseBody.contains("EMAIL_EXISTS")) {
                return;
            }
            if (responseBody.contains("WEAK_PASSWORD")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contrasena debe tener al menos 6 caracteres");
            }
            if (responseBody.contains("INVALID_EMAIL")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo no tiene formato valido");
            }
            if (responseBody.contains("OPERATION_NOT_ALLOWED")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El registro con email y contrasena no esta habilitado en Firebase");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo crear el usuario en Firebase");
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo conectar con Firebase");
        }
    }
}
