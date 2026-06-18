package com.antojito.maps_backend.service;

import com.antojito.maps_backend.dto.ClaimedCouponResponse;
import com.antojito.maps_backend.dto.CouponValidationRequest;
import com.antojito.maps_backend.exception.ResourceNotFoundException;
import com.antojito.maps_backend.model.ClaimedCoupon;
import com.antojito.maps_backend.model.ClaimedCouponStatus;
import com.antojito.maps_backend.model.Coupon;
import com.antojito.maps_backend.model.CouponStatus;
import com.antojito.maps_backend.repository.ClaimedCouponRepository;
import com.antojito.maps_backend.repository.ClientRepository;
import com.antojito.maps_backend.repository.CouponRepository;
import com.antojito.maps_backend.repository.RestauranteRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class CouponClaimService {

    private static final String CLAIM_CODE_PREFIX = "CPN-";

    private final CouponRepository couponRepository;
    private final ClaimedCouponRepository claimedCouponRepository;
    private final ClientRepository clientRepository;
    private final RestauranteRepository restauranteRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    @Transactional
    public ClaimedCouponResponse claimCoupon(UUID clientId, UUID couponId) {
        requireClient(clientId);
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe cupon con uuid " + couponId));

        validateClaimAvailability(clientId, coupon);

        ClaimedCoupon claimedCoupon = claimedCouponRepository.save(ClaimedCoupon.builder()
                .couponId(coupon.getUuid())
                .clientId(clientId)
                .claimCode(generateClaimCode())
                .status(ClaimedCouponStatus.CLAIMED)
                .build());

        long claimedCount = claimedCouponRepository.countByCouponId(coupon.getUuid());
        if (claimedCount >= coupon.getMaxQuantity()) {
            coupon.setStatus(CouponStatus.SOLD_OUT);
            couponRepository.save(coupon);
        }

        auditLogService.logCouponClaim(clientId, coupon.getUuid(), claimedCoupon.getClaimCode());
        return toResponse(claimedCoupon, coupon);
    }

    @Transactional(readOnly = true)
    public List<ClaimedCouponResponse> findClaimedByClient(UUID clientId) {
        requireClient(clientId);
        return claimedCouponRepository.findByClientIdOrderByClaimedAtDesc(clientId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ClaimedCouponResponse validateAndUseCoupon(UUID restaurantId, CouponValidationRequest request) {
        requireRestaurant(restaurantId);
        UUID ownerUuid = resolveOwnerUuid(request.getOwnerUuid(), request.getOwnerMail());
        validateOwnerRestaurantRelationship(ownerUuid, restaurantId);

        String claimCode = request.getClaimCode().trim();
        ClaimedCoupon claimedCoupon = claimedCouponRepository.findByClaimCode(claimCode)
                .orElseThrow(() -> new ResourceNotFoundException("No existe cupon reclamado con codigo " + claimCode));

        Coupon coupon = couponRepository.findById(claimedCoupon.getCouponId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe cupon con uuid " + claimedCoupon.getCouponId()));

        validateCouponCanBeUsed(restaurantId, claimedCoupon, coupon);

        claimedCoupon.setStatus(ClaimedCouponStatus.USED);
        claimedCoupon.setUsedAt(LocalDateTime.now());
        ClaimedCoupon usedCoupon = claimedCouponRepository.save(claimedCoupon);

        auditLogService.logCouponUse(
                ownerUuid,
                restaurantId,
                coupon.getUuid(),
                usedCoupon.getClientId(),
                usedCoupon.getClaimCode());

        return toResponse(usedCoupon, coupon);
    }

    private void validateClaimAvailability(UUID clientId, Coupon coupon) {
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cupon no esta disponible para reclamo");
        }

        LocalDate today = LocalDate.now();
        if (coupon.getStartDate().isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cupon aun no esta vigente");
        }

        if (coupon.getExpirationDate().isBefore(today)) {
            coupon.setStatus(CouponStatus.EXPIRED);
            couponRepository.save(coupon);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cupon ya expiro");
        }

        if (claimedCouponRepository.existsByCouponIdAndClientId(coupon.getUuid(), clientId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El cliente ya reclamo este cupon");
        }

        long claimedCount = claimedCouponRepository.countByCouponId(coupon.getUuid());
        if (claimedCount >= coupon.getMaxQuantity()) {
            coupon.setStatus(CouponStatus.SOLD_OUT);
            couponRepository.save(coupon);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cupon ya no tiene disponibilidad");
        }
    }

    private void requireClient(UUID clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("No existe cliente con uuid " + clientId);
        }
    }

    private void requireRestaurant(UUID restaurantId) {
        if (!restauranteRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("No existe restaurante con uuid " + restaurantId);
        }
    }

    private void validateCouponCanBeUsed(UUID restaurantId, ClaimedCoupon claimedCoupon, Coupon coupon) {
        if (!coupon.getRestaurantId().equals(restaurantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El codigo no pertenece a este restaurante");
        }

        if (claimedCoupon.getStatus() == ClaimedCouponStatus.USED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El cupon ya fue utilizado");
        }

        LocalDate today = LocalDate.now();
        if (coupon.getStartDate().isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cupon aun no esta vigente");
        }

        if (coupon.getExpirationDate().isBefore(today)) {
            coupon.setStatus(CouponStatus.EXPIRED);
            couponRepository.save(coupon);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cupon ya expiro");
        }
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
                    "El owner no tiene permisos para validar cupones en este restaurante");
        }
    }

    private String generateClaimCode() {
        String claimCode;
        do {
            claimCode = CLAIM_CODE_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        } while (claimedCouponRepository.findByClaimCode(claimCode).isPresent());
        return claimCode;
    }

    private String normalizeMail(String mail) {
        if (mail == null) {
            return null;
        }

        String normalized = mail.trim().toLowerCase();
        return normalized.isBlank() ? null : normalized;
    }

    private ClaimedCouponResponse toResponse(ClaimedCoupon claimedCoupon) {
        Coupon coupon = couponRepository.findById(claimedCoupon.getCouponId()).orElse(null);
        return toResponse(claimedCoupon, coupon);
    }

    private ClaimedCouponResponse toResponse(ClaimedCoupon claimedCoupon, Coupon coupon) {
        return ClaimedCouponResponse.builder()
                .uuid(claimedCoupon.getUuid())
                .couponId(claimedCoupon.getCouponId())
                .restaurantId(coupon != null ? coupon.getRestaurantId() : null)
                .clientId(claimedCoupon.getClientId())
                .claimCode(claimedCoupon.getClaimCode())
                .status(claimedCoupon.getStatus())
                .couponName(coupon != null ? coupon.getName() : null)
                .couponDescription(coupon != null ? coupon.getDescription() : null)
                .expirationDate(coupon != null ? coupon.getExpirationDate() : null)
                .claimedAt(claimedCoupon.getClaimedAt())
                .usedAt(claimedCoupon.getUsedAt())
                .build();
    }
}
