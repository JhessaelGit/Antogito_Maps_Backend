package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.AddPointsRequest;
import com.antojito.maps_backend.dto.LoyaltyResponse;
import com.antojito.maps_backend.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loyalty")
@CrossOrigin(originPatterns = "${app.cors.allowed-origins:*}")
@RequiredArgsConstructor
@Tag(name = "Loyalty", description = "Gestion de fidelizacion de clientes")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/{clientId}")
    @Operation(summary = "Obtener perfil de fidelizacion de cliente")
    public ResponseEntity<LoyaltyResponse> getLoyaltyProfile(@PathVariable UUID clientId) {
        return ResponseEntity.ok(loyaltyService.getProfile(clientId));
    }

    @PostMapping("/add-points")
    @Operation(summary = "Agregar puntos de fidelizacion manualmente")
    public ResponseEntity<LoyaltyResponse> addPoints(@Valid @RequestBody AddPointsRequest request) {
        LoyaltyResponse response = loyaltyService.addPoints(request.getClientId(), request.getPoints(), request.getReason());
        return ResponseEntity.ok(response);
    }
}

