package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.DashboardResponse;
import com.antojito.maps_backend.dto.OwnerAuthorizationRequest;
import com.antojito.maps_backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Metricas analiticas por restaurante")
public class DashboardController {

    private final DashboardService dashboardService;

    @PostMapping("/restaurant/{restaurantId}")
    @Operation(summary = "Obtener dashboard analitico del restaurante")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Dashboard obtenido correctamente",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = DashboardResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos invalidos"),
        @ApiResponse(responseCode = "403", description = "Owner sin permisos sobre el restaurante"),
        @ApiResponse(responseCode = "404", description = "No existe owner o restaurante")
    })
    public ResponseEntity<DashboardResponse> getRestaurantDashboard(
            @Parameter(description = "UUID del restaurante")
            @PathVariable UUID restaurantId,
            @Valid @RequestBody OwnerAuthorizationRequest request) {
        return ResponseEntity.ok(dashboardService.getRestaurantDashboard(restaurantId, request));
    }
}
