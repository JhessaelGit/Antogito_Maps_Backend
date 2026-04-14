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
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RestauranteService {

    private final RestauranteRepository repository;
    private final AuditLogService auditLogService;
    private final JdbcTemplate jdbcTemplate;
    private final R2StorageService r2StorageService;

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

        if (isDataUrl(request.getImagenUrl())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "imagenUrl debe ser una URL publica; usa /restaurant/upload-image para subir archivos");
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

    @Transactional(readOnly = true)
    public String uploadImage(MultipartFile file, String restaurantName) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes enviar un archivo de imagen");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen no puede exceder 5 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo debe ser una imagen valida");
        }

        try {
            return r2StorageService.uploadMultipartImage(
                    restaurantName,
                    file.getOriginalFilename(),
                    contentType,
                    file.getBytes());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo subir la imagen");
        }
    }

    private boolean isDataUrl(String value) {
        return value != null && value.startsWith("data:");
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