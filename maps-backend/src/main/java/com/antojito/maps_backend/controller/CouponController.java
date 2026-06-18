package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.CouponRequest;
import com.antojito.maps_backend.dto.CouponResponse;
import com.antojito.maps_backend.dto.OwnerAuthorizationRequest;
import com.antojito.maps_backend.service.CouponService;
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
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coupon")
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Gestion CRUD de cupones por restaurante")
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/restaurant/{restaurantId}")
    @Operation(summary = "Listar cupones por restaurante")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Listado obtenido correctamente",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CouponResponse.class))),
        @ApiResponse(responseCode = "404", description = "No existe restaurante con ese UUID")
    })
    public ResponseEntity<List<CouponResponse>> getCouponsByRestaurant(
            @Parameter(description = "UUID del restaurante")
            @PathVariable UUID restaurantId) {
        return ResponseEntity.ok(couponService.findByRestaurant(restaurantId));
    }

    @GetMapping("/restaurant/{restaurantId}/{couponId}")
    @Operation(summary = "Obtener cupon por restaurante")
    public ResponseEntity<CouponResponse> getCouponByRestaurant(
            @Parameter(description = "UUID del restaurante")
            @PathVariable UUID restaurantId,
            @Parameter(description = "UUID del cupon")
            @PathVariable UUID couponId) {
        return ResponseEntity.ok(couponService.findById(restaurantId, couponId));
    }

    @PostMapping("/restaurant/{restaurantId}")
    @Operation(summary = "Crear cupon para un restaurante")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Cupon creado correctamente",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CouponResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos invalidos"),
        @ApiResponse(responseCode = "403", description = "Owner sin permisos sobre el restaurante"),
        @ApiResponse(responseCode = "404", description = "No existe owner, cliente o restaurante")
    })
    public ResponseEntity<CouponResponse> createCoupon(
            @Parameter(description = "UUID del restaurante")
            @PathVariable UUID restaurantId,
            @Valid @RequestBody CouponRequest request) {
        CouponResponse created = couponService.create(restaurantId, request);
        URI location = URI.create("/coupon/restaurant/" + restaurantId + "/" + created.getUuid());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(location)
                .body(created);
    }

    @PutMapping("/restaurant/{restaurantId}/{couponId}")
    @Operation(summary = "Editar cupon de un restaurante")
    public ResponseEntity<CouponResponse> updateCoupon(
            @Parameter(description = "UUID del restaurante")
            @PathVariable UUID restaurantId,
            @Parameter(description = "UUID del cupon")
            @PathVariable UUID couponId,
            @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(couponService.update(restaurantId, couponId, request));
    }

    @PatchMapping("/restaurant/{restaurantId}/{couponId}/pause")
    @Operation(summary = "Pausar cupon de un restaurante")
    public ResponseEntity<CouponResponse> pauseCoupon(
            @Parameter(description = "UUID del restaurante")
            @PathVariable UUID restaurantId,
            @Parameter(description = "UUID del cupon")
            @PathVariable UUID couponId,
            @Valid @RequestBody OwnerAuthorizationRequest request) {
        return ResponseEntity.ok(couponService.pause(restaurantId, couponId, request));
    }

    @DeleteMapping("/restaurant/{restaurantId}/{couponId}")
    @Operation(summary = "Eliminar cupon de un restaurante")
    public ResponseEntity<Void> deleteCoupon(
            @Parameter(description = "UUID del restaurante")
            @PathVariable UUID restaurantId,
            @Parameter(description = "UUID del cupon")
            @PathVariable UUID couponId,
            @Valid @RequestBody OwnerAuthorizationRequest request) {
        couponService.delete(restaurantId, couponId, request);
        return ResponseEntity.noContent().build();
    }
}
