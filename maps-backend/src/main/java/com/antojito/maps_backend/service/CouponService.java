package com.antojito.maps_backend.service;

import com.antojito.maps_backend.dto.CouponRequest;
import com.antojito.maps_backend.dto.CouponResponse;
import com.antojito.maps_backend.dto.OwnerAuthorizationRequest;
import com.antojito.maps_backend.exception.ResourceNotFoundException;
import com.antojito.maps_backend.model.Coupon;
import com.antojito.maps_backend.model.CouponStatus;
import com.antojito.maps_backend.repository.ClientRepository;
import com.antojito.maps_backend.repository.CouponRepository;
import com.antojito.maps_backend.repository.RestauranteRepository;
import java.time.LocalDate;
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
public class CouponService {

    private final CouponRepository couponRepository;
    private final RestauranteRepository restauranteRepository;
    private final ClientRepository clientRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<CouponResponse> findByRestaurant(UUID restaurantId) {
        requireRestaurant(restaurantId);
        return couponRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CouponResponse findById(UUID restaurantId, UUID couponId) {
        requireRestaurant(restaurantId);
        return toResponse(requireCouponBelongsToRestaurant(couponId, restaurantId));
    }

    @Transactional
    public CouponResponse create(UUID restaurantId, CouponRequest request) {
        requireRestaurant(restaurantId);
        UUID ownerUuid = resolveOwnerUuid(request.getOwnerUuid(), request.getOwnerMail());
        validateOwnerRestaurantRelationship(ownerUuid, restaurantId);
        validateCouponData(request);

        Coupon coupon = couponRepository.save(toEntity(restaurantId, request));
        auditLogService.logCouponCreate(ownerUuid, restaurantId, coupon.getUuid());
        return toResponse(coupon);
    }

    @Transactional
    public CouponResponse update(UUID restaurantId, UUID couponId, CouponRequest request) {
        requireRestaurant(restaurantId);
        UUID ownerUuid = resolveOwnerUuid(request.getOwnerUuid(), request.getOwnerMail());
        validateOwnerRestaurantRelationship(ownerUuid, restaurantId);
        validateCouponData(request);

        Coupon coupon = requireCouponBelongsToRestaurant(couponId, restaurantId);
        coupon.setClientId(request.getClientId());
        coupon.setName(request.getName().trim());
        coupon.setDescription(trimToNull(request.getDescription()));
        coupon.setStartDate(request.getStartDate());
        coupon.setExpirationDate(request.getExpirationDate());
        coupon.setMaxQuantity(request.getMaxQuantity());
        coupon.setDiscountType(request.getDiscountType().trim());
        coupon.setStatus(request.getStatus() == null ? CouponStatus.ACTIVE : request.getStatus());
        coupon.setQrCode(trimToNull(request.getQrCode()));

        Coupon updated = couponRepository.save(coupon);
        auditLogService.logCouponUpdate(ownerUuid, restaurantId, updated.getUuid());
        return toResponse(updated);
    }

    @Transactional
    public CouponResponse pause(UUID restaurantId, UUID couponId, OwnerAuthorizationRequest request) {
        requireRestaurant(restaurantId);
        UUID ownerUuid = resolveOwnerUuid(request.getOwnerUuid(), request.getOwnerMail());
        validateOwnerRestaurantRelationship(ownerUuid, restaurantId);

        Coupon coupon = requireCouponBelongsToRestaurant(couponId, restaurantId);
        coupon.setStatus(CouponStatus.PAUSED);
        Coupon paused = couponRepository.save(coupon);
        auditLogService.logCouponPause(ownerUuid, restaurantId, paused.getUuid());
        return toResponse(paused);
    }

    @Transactional
    public void delete(UUID restaurantId, UUID couponId, OwnerAuthorizationRequest request) {
        requireRestaurant(restaurantId);
        UUID ownerUuid = resolveOwnerUuid(request.getOwnerUuid(), request.getOwnerMail());
        validateOwnerRestaurantRelationship(ownerUuid, restaurantId);

        Coupon coupon = requireCouponBelongsToRestaurant(couponId, restaurantId);
        couponRepository.delete(coupon);
        auditLogService.logCouponDelete(ownerUuid, restaurantId, couponId);
    }

    private void validateCouponData(CouponRequest request) {
        if (request.getExpirationDate().isBefore(request.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha de expiracion no puede ser anterior a la fecha de inicio");
        }

        if (request.getExpirationDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede crear o actualizar un cupon expirado");
        }

        if (request.getMaxQuantity() == null || request.getMaxQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede crear o actualizar un cupon agotado");
        }

        if (request.getStatus() == CouponStatus.EXPIRED || request.getStatus() == CouponStatus.SOLD_OUT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se puede crear o actualizar un cupon con estado expirado o agotado");
        }

        if (request.getClientId() != null && !clientRepository.existsById(request.getClientId())) {
            throw new ResourceNotFoundException("No existe cliente con uuid " + request.getClientId());
        }
    }

    private void requireRestaurant(UUID restaurantId) {
        if (!restauranteRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("No existe restaurante con uuid " + restaurantId);
        }
    }

    private Coupon requireCouponBelongsToRestaurant(UUID couponId, UUID restaurantId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe cupon con uuid " + couponId));
        if (!coupon.getRestaurantId().equals(restaurantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe cupon para ese restaurante");
        }
        return coupon;
    }

    private UUID resolveOwnerUuid(UUID ownerUuid, String ownerMailValue) {
        String ownerMail = normalizeMail(ownerMailValue);

        if (ownerUuid == null && ownerMail == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes enviar ownerUuid u ownerMail");
        }

        if (ownerUuid != null) {
            requireOwner(ownerUuid);
            if (ownerMail != null) {
                validateOwnerIdentity(ownerUuid, ownerMail);
            }
            return ownerUuid;
        }

        return findOwnerUuidByMail(ownerMail);
    }

    private void requireOwner(UUID ownerUuid) {
        Integer matches = jdbcTemplate.queryForObject(
                "select count(*) from owner_account where uuid = ?",
                Integer.class,
                ownerUuid);

        if (matches == null || matches == 0) {
            throw new ResourceNotFoundException("No existe owner con uuid " + ownerUuid);
        }
    }

    private void validateOwnerIdentity(UUID ownerUuid, String ownerMail) {
        Integer matches = jdbcTemplate.queryForObject(
                "select count(*) from owner_account where uuid = ? and mail = ?",
                Integer.class,
                ownerUuid,
                ownerMail);

        if (matches == null || matches == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ownerUuid y ownerMail no corresponden al mismo owner");
        }
    }

    private UUID findOwnerUuidByMail(String ownerMail) {
        try {
            UUID ownerUuid = jdbcTemplate.queryForObject(
                    "select uuid from owner_account where mail = ?",
                    UUID.class,
                    ownerMail);
            if (ownerUuid == null) {
                throw new ResourceNotFoundException("No existe owner con mail " + ownerMail);
            }
            return ownerUuid;
        } catch (EmptyResultDataAccessException exception) {
            throw new ResourceNotFoundException("No existe owner con mail " + ownerMail);
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
                    "El owner no tiene permisos para gestionar cupones en este restaurante");
        }
    }

    private Coupon toEntity(UUID restaurantId, CouponRequest request) {
        return Coupon.builder()
                .restaurantId(restaurantId)
                .clientId(request.getClientId())
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .startDate(request.getStartDate())
                .expirationDate(request.getExpirationDate())
                .maxQuantity(request.getMaxQuantity())
                .discountType(request.getDiscountType().trim())
                .status(request.getStatus() == null ? CouponStatus.ACTIVE : request.getStatus())
                .qrCode(trimToNull(request.getQrCode()))
                .build();
    }

    private CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.builder()
                .uuid(coupon.getUuid())
                .restaurantId(coupon.getRestaurantId())
                .clientId(coupon.getClientId())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .startDate(coupon.getStartDate())
                .expirationDate(coupon.getExpirationDate())
                .maxQuantity(coupon.getMaxQuantity())
                .discountType(coupon.getDiscountType())
                .status(coupon.getStatus())
                .qrCode(coupon.getQrCode())
                .createdAt(coupon.getCreatedAt())
                .build();
    }

    private String normalizeMail(String mail) {
        if (mail == null) {
            return null;
        }

        String normalized = mail.trim().toLowerCase();
        return normalized.isBlank() ? null : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
