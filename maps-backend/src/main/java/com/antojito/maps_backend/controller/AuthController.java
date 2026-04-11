package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.ApiMessageResponse;
import com.antojito.maps_backend.dto.RestaurantLoginRequest;
import com.antojito.maps_backend.dto.RestaurantLogoutRequest;
import com.antojito.maps_backend.dto.RestaurantRegistryRequest;
import com.antojito.maps_backend.repository.RestauranteRepository;
import com.antojito.maps_backend.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    private final RestauranteRepository restauranteRepository;

    public AuthController(
            AuditLogService auditLogService,
            JdbcTemplate jdbcTemplate,
            RestauranteRepository restauranteRepository) {
        this.auditLogService = auditLogService;
        this.jdbcTemplate = jdbcTemplate;
        this.restauranteRepository = restauranteRepository;
    }

    @PostMapping("/login")
    @Operation(summary = "Login de owner", description = "Valida credenciales de owner_restaurant")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Login correcto",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiMessageResponse.class))),
        @ApiResponse(responseCode = "401", description = "Credenciales invalidas")
    })
    public ResponseEntity<ApiMessageResponse> login(@Valid @RequestBody RestaurantLoginRequest request) {
        Integer matches = jdbcTemplate.queryForObject(
                "select count(*) from owner_restaurant where mail = ? and password = ?",
                Integer.class,
                request.getMail(),
                request.getPassword());

        if (matches == null || matches == 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }

        auditLogService.logLogin(request.getMail());
        return ResponseEntity.ok(new ApiMessageResponse("login correcto"));
    }

    @PostMapping("/registry")
    @Operation(summary = "Registrar owner", description = "Registra un owner para un restaurante existente")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Owner registrado",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiMessageResponse.class))),
        @ApiResponse(responseCode = "404", description = "No existe restaurante con ese UUID"),
        @ApiResponse(responseCode = "400", description = "Owner ya registrado para ese restaurante")
    })
    public ResponseEntity<ApiMessageResponse> registry(@Valid @RequestBody RestaurantRegistryRequest request) {
        if (!restauranteRepository.existsById(request.getRestaurantId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe restaurante con uuid " + request.getRestaurantId());
        }

        try {
            jdbcTemplate.update(
                    "insert into owner_restaurant (id_restaurant, mail, password) values (?, ?, ?)",
                    request.getRestaurantId(),
                    request.getMail(),
                    request.getPassword());
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner ya registrado para este restaurante");
        }

        auditLogService.logOwnerRegistry(request.getRestaurantId(), request.getMail());
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