package com.antojito.maps_backend.service;

import com.antojito.maps_backend.dto.PromotionCreateRequest;
import com.antojito.maps_backend.dto.PromotionResponse;
import com.antojito.maps_backend.exception.ResourceNotFoundException;
import com.antojito.maps_backend.model.Promotion;
import com.antojito.maps_backend.repository.PromotionRepository;
import com.antojito.maps_backend.repository.RestauranteRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final RestauranteRepository restauranteRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<PromotionResponse> findActiveByRestaurant(UUID restaurantId) {
        requireRestaurant(restaurantId);

        return promotionRepository
                .findByRestaurantIdAndIsActivePromotionTrueOrderByDateEndPromotionAsc(restaurantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PromotionResponse create(UUID restaurantId, PromotionCreateRequest request) {
        requireRestaurant(restaurantId);

        if (request.getDateEndPromotion().isBefore(request.getDateStartPromotion())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        UUID ownerUuid = findOwnerUuidByMail(request.getOwnerMail());
        validateOwnerRestaurantRelationship(ownerUuid, restaurantId);

        Promotion created = promotionRepository.save(toEntity(restaurantId, request));
        return toResponse(created);
    }

    private void requireRestaurant(UUID restaurantId) {
        if (!restauranteRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("No existe restaurante con uuid " + restaurantId);
        }
    }

    private UUID findOwnerUuidByMail(String ownerMail) {
        String normalizedOwnerMail = ownerMail == null ? null : ownerMail.trim().toLowerCase();
        try {
            UUID ownerUuid = jdbcTemplate.queryForObject(
                    "select uuid from owner_account where mail = ?",
                    UUID.class,
                    normalizedOwnerMail);
            if (ownerUuid == null) {
                throw new ResourceNotFoundException("No existe owner con mail " + normalizedOwnerMail);
            }
            return ownerUuid;
        } catch (EmptyResultDataAccessException exception) {
            throw new ResourceNotFoundException("No existe owner con mail " + normalizedOwnerMail);
        }
    }

    private void validateOwnerRestaurantRelationship(UUID ownerUuid, UUID restaurantId) {
        Integer matches = jdbcTemplate.queryForObject(
                "select count(*) from owner_restaurant where id_owner = ? and id_restaurant = ?",
                Integer.class,
                ownerUuid,
                restaurantId);

        if (matches == null || matches == 0) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El owner no tiene permisos para crear promociones en este restaurante");
        }
    }

    private Promotion toEntity(UUID restaurantId, PromotionCreateRequest request) {
        return Promotion.builder()
                .restaurantId(restaurantId)
                .title(request.getTitle())
                .description(request.getDescription())
                .percentDiscount(request.getPercentDiscount())
                .dateStartPromotion(request.getDateStartPromotion())
                .dateEndPromotion(request.getDateEndPromotion())
                .isActivePromotion(request.getIsActivePromotion())
                .build();
    }

    private PromotionResponse toResponse(Promotion promotion) {
        return PromotionResponse.builder()
                .uuid(promotion.getUuid())
                .restaurantId(promotion.getRestaurantId())
                .title(promotion.getTitle())
                .description(promotion.getDescription())
                .percentDiscount(promotion.getPercentDiscount())
                .dateStartPromotion(promotion.getDateStartPromotion())
                .dateEndPromotion(promotion.getDateEndPromotion())
                .isActivePromotion(promotion.getIsActivePromotion())
                .build();
    }
}
