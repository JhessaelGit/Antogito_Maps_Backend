package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.RestauranteCreateRequest;
import com.antojito.maps_backend.dto.RestauranteResponse;
import com.antojito.maps_backend.service.RestauranteService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping({"/api/v1/restaurantes"})
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
public class RestauranteController {

    private final RestauranteService restauranteService;

    public RestauranteController(RestauranteService restauranteService) {
        this.restauranteService = restauranteService;
    }

    @GetMapping
    public ResponseEntity<List<RestauranteResponse>> listarRestaurantes() {
        return ResponseEntity.ok(restauranteService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestauranteResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(restauranteService.findById(id));
    }

    @PostMapping
    public ResponseEntity<RestauranteResponse> crearRestaurante(
            @Valid @RequestBody RestauranteCreateRequest request) {
        RestauranteResponse created = restauranteService.create(request);
        URI location = URI.create("/api/v1/restaurantes/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarRestaurante(@PathVariable Long id) {
        restauranteService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}