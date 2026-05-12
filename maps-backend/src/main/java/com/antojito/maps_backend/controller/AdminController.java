package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.AdminCreateRequest;
import com.antojito.maps_backend.dto.AdminLoginResponse;
import com.antojito.maps_backend.dto.AdminResponse;
import com.antojito.maps_backend.dto.AdminRestaurantBlockRequest;
import com.antojito.maps_backend.dto.AdminUpdateRequest;
import com.antojito.maps_backend.dto.ApiMessageResponse;
import com.antojito.maps_backend.dto.ClientLoginRequest;
import com.antojito.maps_backend.dto.RestauranteResponse;
import com.antojito.maps_backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administracion de administradores y moderacion de restaurantes")
public class AdminController {

    private static final String HEADER_ADMIN_ID = "X-Admin-Id";

    private final AdminService adminService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${firebase.api-key}")
    private String firebaseApiKey;

    // ─────────────────────────────────────────────────────────────
    // LOGIN: autentica con Firebase REST + valida admin en BD
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/login")
    @Operation(summary = "Login admin", description = "Autentica con email y password. Firebase gestionado por el backend.")
    public ResponseEntity<AdminLoginResponse> login(@Valid @RequestBody ClientLoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // 1. Autenticar contra Firebase REST
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + firebaseApiKey;
        try {
            restTemplate.postForEntity(url,
                    Map.of("email", email, "password", request.getPassword(), "returnSecureToken", true),
                    Map.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contrasena incorrectos");
        }

        // 2. Buscar admin en BD
        return ResponseEntity.ok(adminService.login(email));
    }

    // ─────────────────────────────────────────────────────────────
    // CREATE: crea admin (con o sin X-Admin-Id para bootstrap)
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/create")
    @Operation(summary = "Crear admin", description = "Solo un admin activo puede crear otro. Si no existe ninguno, el bootstrap funciona sin header.")
    public ResponseEntity<AdminResponse> createAdmin(
            @RequestHeader(value = HEADER_ADMIN_ID, required = false) String actorAdminIdHeader,
            @Valid @RequestBody AdminCreateRequest request) {
        UUID actorAdminId = parseOptionalUuid(actorAdminIdHeader);
        AdminResponse created = adminService.createAdmin(actorAdminId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/edit")
    @Operation(summary = "Editar admin")
    public ResponseEntity<AdminResponse> editOwnProfile(
            @RequestHeader(HEADER_ADMIN_ID) String actorAdminIdHeader,
            @Valid @RequestBody AdminUpdateRequest request) {
        UUID actorAdminId = parseRequiredUuid(actorAdminIdHeader, HEADER_ADMIN_ID);
        AdminResponse updated = adminService.updateOwnProfile(actorAdminId, request.getMail(), request.getPassword());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Eliminar admin (borrado logico)")
    public ResponseEntity<ApiMessageResponse> deleteAdmin(
            @RequestHeader(HEADER_ADMIN_ID) String actorAdminIdHeader,
            @Parameter(description = "UUID del admin objetivo")
            @PathVariable UUID id) {
        UUID actorAdminId = parseRequiredUuid(actorAdminIdHeader, HEADER_ADMIN_ID);
        adminService.softDeleteAdmin(actorAdminId, id);
        return ResponseEntity.ok(new ApiMessageResponse("admin eliminado"));
    }

    @GetMapping("/all")
    @Operation(summary = "Listar admins activos")
    public ResponseEntity<List<AdminResponse>> listActiveAdmins() {
        return ResponseEntity.ok(adminService.findActiveAdmins());
    }

    @GetMapping("/deleted")
    @Operation(summary = "Listar admins eliminados")
    public ResponseEntity<List<AdminResponse>> listDeletedAdmins() {
        return ResponseEntity.ok(adminService.findDeletedAdmins());
    }

    @GetMapping("/restaurants")
    @Operation(summary = "Listar restaurantes para moderacion")
    public ResponseEntity<List<RestauranteResponse>> listRestaurants(
            @RequestHeader(HEADER_ADMIN_ID) String actorAdminIdHeader) {
        UUID actorAdminId = parseRequiredUuid(actorAdminIdHeader, HEADER_ADMIN_ID);
        return ResponseEntity.ok(adminService.findAllRestaurants(actorAdminId));
    }

    @PatchMapping("/restaurants/{id}/block")
    @Operation(summary = "Bloquear o desbloquear restaurante")
    public ResponseEntity<RestauranteResponse> updateRestaurantBlockStatus(
            @RequestHeader(HEADER_ADMIN_ID) String actorAdminIdHeader,
            @PathVariable UUID id,
            @Valid @RequestBody AdminRestaurantBlockRequest request) {
        UUID actorAdminId = parseRequiredUuid(actorAdminIdHeader, HEADER_ADMIN_ID);
        RestauranteResponse updated = adminService.updateRestaurantBlockStatus(actorAdminId, id, request);
        return ResponseEntity.ok(updated);
    }

    private UUID parseRequiredUuid(String rawUuid, String fieldName) {
        if (rawUuid == null || rawUuid.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Header " + fieldName + " requerido");
        try { return UUID.fromString(rawUuid.trim()); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " invalido"); }
    }

    private UUID parseOptionalUuid(String rawUuid) {
        if (rawUuid == null || rawUuid.isBlank()) return null;
        try { return UUID.fromString(rawUuid.trim()); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, HEADER_ADMIN_ID + " invalido"); }
    }
}
