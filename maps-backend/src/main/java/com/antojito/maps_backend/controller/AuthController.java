package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.ApiMessageResponse;
import com.antojito.maps_backend.dto.LoginRequest;
import com.antojito.maps_backend.service.AuditLogService;
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
public class AuthController {

    private final AuditLogService auditLogService;

    public AuthController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PostMapping({"", "/login"})
    public ResponseEntity<ApiMessageResponse> registrarLogin(@Valid @RequestBody LoginRequest request) {
        auditLogService.logLogin(request.getEmail());
        return ResponseEntity.ok(new ApiMessageResponse("login registrado"));
    }
}