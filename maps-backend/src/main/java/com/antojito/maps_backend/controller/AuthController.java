package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.ApiMessageResponse;
import com.antojito.maps_backend.dto.FirebaseLoginRequest;
import com.antojito.maps_backend.dto.RestaurantLoginResponse;
import com.antojito.maps_backend.dto.RestaurantLogoutRequest;
import com.antojito.maps_backend.service.AuditLogService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;

@RestController
@RequestMapping("/restaurant")
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
@Tag(name = "Restaurant Auth", description = "Autenticacion de owners de restaurantes (con Firebase)")
public class AuthController {

    private final AuditLogService auditLogService;
    private final JdbcTemplate jdbcTemplate;

    public AuthController(
            AuditLogService auditLogService,
            JdbcTemplate jdbcTemplate) {
        this.auditLogService = auditLogService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/login")
    @Operation(summary = "Login de owner con Firebase", description = "Valida token de Firebase y obtiene credenciales del owner")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Login correcto",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantLoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Credenciales o token invalido")
    })
    public ResponseEntity<RestaurantLoginResponse> login(@Valid @RequestBody FirebaseLoginRequest request) {
        String email;
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());
            email = decodedToken.getEmail();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de Firebase invalido");
        }

        UUID ownerUuid;
        try {
            ownerUuid = jdbcTemplate.queryForObject(
                    "select uuid from owner_account where mail = ?",
                    UUID.class,
                    email);
        } catch (EmptyResultDataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El owner no esta registrado en el sistema");
        }

        List<UUID> restaurantIds = jdbcTemplate.queryForList(
                "select id_restaurant from owner_restaurant where id_owner = ?",
                UUID.class,
                ownerUuid);

        auditLogService.logLogin(email);
        return ResponseEntity.ok(new RestaurantLoginResponse(
                ownerUuid,
                email,
                restaurantIds,
                "login correcto"));
    }

    @PostMapping("/registry")
    @Operation(summary = "Registrar owner con Firebase", description = "Registra owner extraido del token de Firebase en la BD local")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Owner registrado",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiMessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Owner ya registrado"),
        @ApiResponse(responseCode = "401", description = "Token de Firebase invalido")
    })
    public ResponseEntity<ApiMessageResponse> registry(@Valid @RequestBody FirebaseLoginRequest request) {
        String email;
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());
            email = decodedToken.getEmail();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de Firebase invalido");
        }

        try {
            // Se inserta "FIREBASE_AUTH" como password ficticio porque la DB podria requerirlo (NOT NULL)
            jdbcTemplate.update(
                    "insert into owner_account (uuid, mail, password) values (?, ?, ?)",
                    UUID.randomUUID(),
                    email,
                    "FIREBASE_AUTH");
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un owner con ese mail");
        }

        auditLogService.logOwnerRegistration(email);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiMessageResponse("owner registrado"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout de owner", description = "Registra el cierre de sesion")
    @ApiResponse(
            responseCode = "200",
            description = "Logout registrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiMessageResponse.class)))
    public ResponseEntity<ApiMessageResponse> logout(@Valid @RequestBody RestaurantLogoutRequest request) {
        auditLogService.logLogout(request.getMail());
        return ResponseEntity.ok(new ApiMessageResponse("logout registrado"));
    }
}