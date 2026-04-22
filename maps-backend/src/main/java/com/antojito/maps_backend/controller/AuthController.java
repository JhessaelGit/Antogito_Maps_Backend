package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.ApiMessageResponse;
import com.antojito.maps_backend.dto.RestaurantLoginRequest;
import com.antojito.maps_backend.dto.RestaurantLoginResponse;
import com.antojito.maps_backend.dto.RestaurantLogoutRequest;
import com.antojito.maps_backend.dto.RestaurantRegistryRequest;
import com.antojito.maps_backend.service.AuditLogService;
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
@Tag(name = "Restaurant Auth", description = "Autenticacion de owners de restaurantes")
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
    @Operation(summary = "Login de owner", description = "Valida credenciales de owner_account")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Login correcto",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestaurantLoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Credenciales invalidas")
    })
    public ResponseEntity<RestaurantLoginResponse> login(@Valid @RequestBody RestaurantLoginRequest request) {
        UUID ownerUuid;
        try {
            ownerUuid = jdbcTemplate.queryForObject(
                    "select uuid from owner_account where mail = ? and password = ?",
                    UUID.class,
                    request.getMail(),
                    request.getPassword());
        } catch (EmptyResultDataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }

        if (ownerUuid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }

        List<UUID> restaurantIds = jdbcTemplate.queryForList(
                "select id_restaurant from owner_restaurant where id_owner = ?",
                UUID.class,
                ownerUuid);

        auditLogService.logLogin(request.getMail());
        return ResponseEntity.ok(new RestaurantLoginResponse(
                ownerUuid,
                request.getMail(),
                restaurantIds,
                "login correcto"));
    }

    @PostMapping("/registry")
    @Operation(summary = "Registrar owner", description = "Registra owner sin necesidad de crear restaurante")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Owner registrado",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiMessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Owner ya registrado")
    })
    public ResponseEntity<ApiMessageResponse> registry(@Valid @RequestBody RestaurantRegistryRequest request) {
        try {
            jdbcTemplate.update(
                    "insert into owner_account (uuid, mail, password) values (?, ?, ?)",
                    UUID.randomUUID(),
                    request.getMail(),
                    request.getPassword());
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un owner con ese mail");
        }

        auditLogService.logOwnerRegistration(request.getMail());
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