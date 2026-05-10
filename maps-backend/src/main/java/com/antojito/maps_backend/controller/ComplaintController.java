package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.ComplaintCreateRequest;
import com.antojito.maps_backend.dto.ComplaintResponse;
import com.antojito.maps_backend.dto.ComplaintReviewRequest;
import com.antojito.maps_backend.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/complaint")
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
@Tag(name = "Quejas", description = "Gestion de quejas por restaurantes o promociones")
@RequiredArgsConstructor
public class ComplaintController {

    private static final String HEADER_ADMIN_ID = "X-Admin-Id";
    private final ComplaintService complaintService;

    @PostMapping("/create")
    @Operation(summary = "Crear queja", description = "Permite a un usuario crear una queja de restaurante o promocion")
    public ResponseEntity<ComplaintResponse> createComplaint(@Valid @RequestBody ComplaintCreateRequest request) {
        ComplaintResponse created = complaintService.createComplaint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/admin/all")
    @Operation(summary = "Ver todas las quejas", description = "Permite a los administradores ver todas las quejas")
    public ResponseEntity<List<ComplaintResponse>> getAllComplaints(
            @RequestHeader(HEADER_ADMIN_ID) String actorAdminIdHeader) {
        UUID actorAdminId = parseRequiredUuid(actorAdminIdHeader);
        return ResponseEntity.ok(complaintService.getAllComplaints(actorAdminId));
    }

    @GetMapping("/admin/pending")
    @Operation(summary = "Ver quejas pendientes", description = "Permite a los administradores ver las quejas en estado PENDING")
    public ResponseEntity<List<ComplaintResponse>> getPendingComplaints(
            @RequestHeader(HEADER_ADMIN_ID) String actorAdminIdHeader) {
        UUID actorAdminId = parseRequiredUuid(actorAdminIdHeader);
        return ResponseEntity.ok(complaintService.getPendingComplaints(actorAdminId));
    }

    @PostMapping("/admin/review/{id}")
    @Operation(summary = "Revisar queja", description = "Permite a los administradores aceptar o rechazar una queja")
    public ResponseEntity<ComplaintResponse> reviewComplaint(
            @RequestHeader(HEADER_ADMIN_ID) String actorAdminIdHeader,
            @Parameter(description = "UUID de la queja", example = "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9")
            @PathVariable UUID id,
            @Valid @RequestBody ComplaintReviewRequest request) {
        UUID actorAdminId = parseRequiredUuid(actorAdminIdHeader);
        ComplaintResponse reviewed = complaintService.reviewComplaint(actorAdminId, id, request);
        return ResponseEntity.ok(reviewed);
    }

    private UUID parseRequiredUuid(String rawUuid) {
        if (rawUuid == null || rawUuid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, HEADER_ADMIN_ID + " requerido");
        }
        try {
            return UUID.fromString(rawUuid.trim());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, HEADER_ADMIN_ID + " invalido");
        }
    }
}
