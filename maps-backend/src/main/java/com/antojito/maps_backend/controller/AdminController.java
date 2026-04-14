package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.AdminCreateRequest;
import com.antojito.maps_backend.dto.AdminLoginRequest;
import com.antojito.maps_backend.dto.AdminLoginResponse;
import com.antojito.maps_backend.dto.AdminResponse;
import com.antojito.maps_backend.dto.AdminRestaurantBlockRequest;
import com.antojito.maps_backend.dto.AdminUpdateRequest;
import com.antojito.maps_backend.dto.ApiMessageResponse;
import com.antojito.maps_backend.dto.RestauranteResponse;
import com.antojito.maps_backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
@Tag(name = "Admin", description = "Administracion de administradores y moderacion de restaurantes")
public class AdminController {

    private static final String HEADER_ADMIN_ID = "X-Admin-Id";

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login admin", description = "Autentica un administrador por mail y password")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Login correcto",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminLoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Credenciales invalidas")
    })
    public ResponseEntity<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(adminService.login(request));
    }

    @PostMapping("/create")
    @Operation(summary = "Crear admin", description = "Solo un admin activo puede crear otro admin. Si no existe ninguno, permite bootstrap inicial")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Admin creado",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminResponse.class))),
        @ApiResponse(responseCode = "400", description = "Mail duplicado o invalido"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<AdminResponse> createAdmin(
            @RequestHeader(value = HEADER_ADMIN_ID, required = false) String actorAdminIdHeader,
            @Valid @RequestBody AdminCreateRequest request) {
        UUID actorAdminId = parseOptionalUuid(actorAdminIdHeader);
        AdminResponse created = adminService.createAdmin(actorAdminId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/edit")
    @Operation(summary = "Editar admin", description = "Permite a un admin editar su propio mail y password")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Admin actualizado",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminResponse.class))),
        @ApiResponse(responseCode = "400", description = "Mail duplicado o invalido"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<AdminResponse> editOwnProfile(
            @RequestHeader(HEADER_ADMIN_ID) String actorAdminIdHeader,
            @Valid @RequestBody AdminUpdateRequest request) {
        UUID actorAdminId = parseRequiredUuid(actorAdminIdHeader, HEADER_ADMIN_ID);
        AdminResponse updated = adminService.updateOwnProfile(actorAdminId, request.getMail(), request.getPassword());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Eliminar admin", description = "Realiza borrado logico de otro administrador")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Admin eliminado logicamente",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiMessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Operacion invalida"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Admin no encontrado")
    })
    public ResponseEntity<ApiMessageResponse> deleteAdmin(
            @RequestHeader(HEADER_ADMIN_ID) String actorAdminIdHeader,
            @Parameter(description = "UUID del admin objetivo", example = "f792617d-0d5d-4881-b5f6-679bcf2c37f8")
            @PathVariable UUID id) {
        UUID actorAdminId = parseRequiredUuid(actorAdminIdHeader, HEADER_ADMIN_ID);
        adminService.softDeleteAdmin(actorAdminId, id);
        return ResponseEntity.ok(new ApiMessageResponse("admin eliminado"));
    }

    @GetMapping("/all")
    @Operation(summary = "Listar admins activos", description = "Obtiene todos los administradores activos")
    public ResponseEntity<List<AdminResponse>> listActiveAdmins() {
        return ResponseEntity.ok(adminService.findActiveAdmins());
    }

    @GetMapping("/deleted")
    @Operation(summary = "Listar admins eliminados", description = "Obtiene administradores con borrado logico")
    public ResponseEntity<List<AdminResponse>> listDeletedAdmins() {
        return ResponseEntity.ok(adminService.findDeletedAdmins());
    }

    @GetMapping("/restaurants")
    @Operation(summary = "Listar restaurantes para moderacion", description = "Muestra todos los restaurantes y su estado is_blocked")
    public ResponseEntity<List<RestauranteResponse>> listRestaurants(
            @RequestHeader(HEADER_ADMIN_ID) String actorAdminIdHeader) {
        UUID actorAdminId = parseRequiredUuid(actorAdminIdHeader, HEADER_ADMIN_ID);
        return ResponseEntity.ok(adminService.findAllRestaurants(actorAdminId));
    }

    @PatchMapping("/restaurants/{id}/block")
    @Operation(summary = "Bloquear o desbloquear restaurante", description = "Actualiza el atributo is_blocked de un restaurante")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Estado de bloqueo actualizado",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestauranteResponse.class))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Restaurante no encontrado")
    })
    public ResponseEntity<RestauranteResponse> updateRestaurantBlockStatus(
            @RequestHeader(HEADER_ADMIN_ID) String actorAdminIdHeader,
            @Parameter(description = "UUID del restaurante", example = "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9")
            @PathVariable UUID id,
            @Valid @RequestBody AdminRestaurantBlockRequest request) {
        UUID actorAdminId = parseRequiredUuid(actorAdminIdHeader, HEADER_ADMIN_ID);
        RestauranteResponse updated = adminService.updateRestaurantBlockStatus(actorAdminId, id, request);
        return ResponseEntity.ok(updated);
    }

    private UUID parseRequiredUuid(String rawUuid, String fieldName) {
        if (rawUuid == null || rawUuid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " requerido");
        }
        try {
            return UUID.fromString(rawUuid.trim());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " invalido");
        }
    }

    private UUID parseOptionalUuid(String rawUuid) {
        if (rawUuid == null || rawUuid.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(rawUuid.trim());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, HEADER_ADMIN_ID + " invalido");
        }
    }
}
