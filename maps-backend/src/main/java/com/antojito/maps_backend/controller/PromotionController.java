package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.PromotionCreateRequest;
import com.antojito.maps_backend.dto.PromotionResponse;
import com.antojito.maps_backend.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/promotion")
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
@Tag(name = "Promotions", description = "Gestion de promociones por restaurante")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping("/restaurant/{restaurantId}")
    @Operation(summary = "Listar promociones activas por restaurante")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Listado obtenido correctamente",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = PromotionResponse.class))),
        @ApiResponse(responseCode = "404", description = "No existe restaurante con ese UUID")
    })
    public ResponseEntity<List<PromotionResponse>> getActivePromotionsByRestaurant(
            @Parameter(description = "UUID del restaurante", example = "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9")
            @PathVariable UUID restaurantId) {
        return ResponseEntity.ok(promotionService.findActiveByRestaurant(restaurantId));
    }

    @PostMapping("/restaurant/{restaurantId}")
    @Operation(summary = "Crear promocion para un restaurante")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Promocion creada correctamente",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = PromotionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos invalidos"),
        @ApiResponse(responseCode = "403", description = "Owner sin permisos sobre el restaurante"),
        @ApiResponse(responseCode = "404", description = "No existe owner o restaurante")
    })
    public ResponseEntity<PromotionResponse> createPromotion(
            @Parameter(description = "UUID del restaurante", example = "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9")
            @PathVariable UUID restaurantId,
            @Valid @RequestBody PromotionCreateRequest request) {
        PromotionResponse created = promotionService.create(restaurantId, request);
        URI location = URI.create("/promotion/restaurant/" + restaurantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(location)
                .body(created);
    }
}
