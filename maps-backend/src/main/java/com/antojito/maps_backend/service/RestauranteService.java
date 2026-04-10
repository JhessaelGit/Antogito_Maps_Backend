package com.antojito.maps_backend.service;

import com.antojito.maps_backend.dto.RestauranteCreateRequest;
import com.antojito.maps_backend.dto.RestauranteResponse;
import com.antojito.maps_backend.exception.ResourceNotFoundException;
import com.antojito.maps_backend.model.Restaurante;
import com.antojito.maps_backend.repository.RestauranteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestauranteService {

    private final RestauranteRepository repository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<RestauranteResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RestauranteResponse findById(Long id) {
        Restaurante restaurante = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe restaurante con id " + id));
        return toResponse(restaurante);
    }

    @Transactional
    public RestauranteResponse create(RestauranteCreateRequest request) {
        Restaurante saved = repository.save(toEntity(request));
        auditLogService.logRestaurantRegistration(saved.getId(), saved.getNombre(), saved.getCorreo());
        return toResponse(saved);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("No existe restaurante con id " + id);
        }
        repository.deleteById(id);
    }

    private Restaurante toEntity(RestauranteCreateRequest request) {
        return Restaurante.builder()
                .nombre(request.getNombre())
                .correo(request.getCorreo())
                .contrasena(request.getContrasena())
                .descripcion(request.getDescripcion())
                .imagenUrl(request.getImagenUrl())
                .planSuscripcion(request.getPlanSuscripcion())
                .fechaVencimientoPlan(request.getFechaVencimientoPlan())
                .estadoBloqueo(request.getEstadoBloqueo())
                .lat(request.getLat())
                .lng(request.getLng())
                .latitud(request.getLat())
                .longitud(request.getLng())
                .build();
    }

    private RestauranteResponse toResponse(Restaurante entity) {
        return RestauranteResponse.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .correo(entity.getCorreo())
                .descripcion(entity.getDescripcion())
                .imagenUrl(entity.getImagenUrl())
                .planSuscripcion(entity.getPlanSuscripcion())
                .fechaVencimientoPlan(entity.getFechaVencimientoPlan())
                .estadoBloqueo(entity.getEstadoBloqueo())
                .lat(entity.getLat() != null ? entity.getLat() : entity.getLatitud())
                .lng(entity.getLng() != null ? entity.getLng() : entity.getLongitud())
                .build();
    }
}