package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.RestauranteCreateRequest;
import com.antojito.maps_backend.dto.RestauranteResponse;
import com.antojito.maps_backend.service.RestauranteService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/restaurantes"})
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
@Tag(name = "Restaurantes", description = "Operaciones CRUD de restaurantes")
public class RestauranteController {

    private final RestauranteService restauranteService;

    public RestauranteController(RestauranteService restauranteService) {
        this.restauranteService = restauranteService;
    }

    @GetMapping
    @Operation(summary = "Listar restaurantes", description = "Obtiene la lista completa de restaurantes")
    @ApiResponse(
            responseCode = "200",
            description = "Listado obtenido correctamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestauranteResponse.class)))
    public ResponseEntity<List<RestauranteResponse>> listarRestaurantes() {
        return ResponseEntity.ok(restauranteService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener restaurante por ID", description = "Busca un restaurante especifico por su identificador")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Restaurante encontrado",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestauranteResponse.class))),
        @ApiResponse(responseCode = "404", description = "No existe restaurante con ese ID")
    })
    public ResponseEntity<RestauranteResponse> obtenerPorId(
            @Parameter(description = "ID del restaurante", example = "11") @PathVariable Long id) {
        return ResponseEntity.ok(restauranteService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear restaurante", description = "Registra un nuevo restaurante")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Restaurante creado correctamente",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestauranteResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos invalidos o violacion de restricciones")
    })
    public ResponseEntity<RestauranteResponse> crearRestaurante(
            @Valid @RequestBody RestauranteCreateRequest request) {
        RestauranteResponse created = restauranteService.create(request);
        URI location = URI.create("/api/v1/restaurantes/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar restaurante", description = "Elimina un restaurante por su ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Restaurante eliminado"),
        @ApiResponse(responseCode = "404", description = "No existe restaurante con ese ID")
    })
    public ResponseEntity<Void> eliminarRestaurante(
            @Parameter(description = "ID del restaurante", example = "11") @PathVariable Long id) {
        restauranteService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}