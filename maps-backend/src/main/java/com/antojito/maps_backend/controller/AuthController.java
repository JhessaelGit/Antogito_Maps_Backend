package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.ApiMessageResponse;
import com.antojito.maps_backend.dto.LoginRequest;
import com.antojito.maps_backend.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/auth", "/api/v1/logs"})
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
@Tag(name = "Autenticacion", description = "Endpoints de autenticacion y auditoria de login")
public class AuthController {

    private final AuditLogService auditLogService;

    public AuthController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PostMapping({"", "/login"})
    @Operation(summary = "Registrar login", description = "Registra en auditoria un inicio de sesion")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Login registrado correctamente",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiMessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Email invalido")
    })
    public ResponseEntity<ApiMessageResponse> registrarLogin(@Valid @RequestBody LoginRequest request) {
        auditLogService.logLogin(request.getEmail());
        return ResponseEntity.ok(new ApiMessageResponse("login registrado"));
    }
}