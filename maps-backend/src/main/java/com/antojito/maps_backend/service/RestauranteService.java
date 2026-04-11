package com.antojito.maps_backend.service;

import com.antojito.maps_backend.dto.RestauranteCreateRequest;
import com.antojito.maps_backend.dto.RestauranteResponse;
import com.antojito.maps_backend.exception.ResourceNotFoundException;
import com.antojito.maps_backend.model.Restaurante;
import com.antojito.maps_backend.repository.RestauranteRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestauranteService {

    private final RestauranteRepository repository;
    private final AuditLogService auditLogService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<RestauranteResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RestauranteResponse findById(UUID uuid) {
        Restaurante restaurante = repository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("No existe restaurante con uuid " + uuid));
        return toResponse(restaurante);
    }

    @Transactional
    public RestauranteResponse create(RestauranteCreateRequest request) {
        UUID ownerUuid;
        try {
            ownerUuid = jdbcTemplate.queryForObject(
                    "select uuid from owner_account where mail = ?",
                    UUID.class,
                    request.getOwnerMail());
        } catch (EmptyResultDataAccessException exception) {
            throw new ResourceNotFoundException("No existe owner con mail " + request.getOwnerMail());
        }

        if (ownerUuid == null) {
            throw new ResourceNotFoundException("No existe owner con mail " + request.getOwnerMail());
        }

        Restaurante saved = repository.saveAndFlush(toEntity(request));
        jdbcTemplate.update(
                "insert into owner_restaurant (id_owner, id_restaurant) values (?, ?)",
                ownerUuid,
                saved.getUuid());
        auditLogService.logRestaurantRegistration(saved.getUuid(), saved.getName());
        auditLogService.logOwnerRegistry(saved.getUuid(), request.getOwnerMail());
        return toResponse(saved);
    }

    @Transactional
    public void deleteById(UUID uuid) {
        if (!repository.existsById(uuid)) {
            throw new ResourceNotFoundException("No existe restaurante con uuid " + uuid);
        }
        repository.deleteById(uuid);
    }

    private Restaurante toEntity(RestauranteCreateRequest request) {
        return Restaurante.builder()
                .name(request.getName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .description(request.getDescription())
                .imagenUrl(request.getImagenUrl())
                .planSuscription(request.getPlanSuscription())
                .planExpirationDate(request.getPlanExpirationDate())
                .isBlocked(request.getIsBlocked())
                .category(request.getCategory())
                .build();
    }

    private RestauranteResponse toResponse(Restaurante entity) {
        return RestauranteResponse.builder()
                .uuid(entity.getUuid())
                .name(entity.getName())
                .description(entity.getDescription())
                .imagenUrl(entity.getImagenUrl())
                .planSuscription(entity.getPlanSuscription())
                .planExpirationDate(entity.getPlanExpirationDate())
                .isBlocked(entity.getIsBlocked())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .category(entity.getCategory())
                .build();
    }
}