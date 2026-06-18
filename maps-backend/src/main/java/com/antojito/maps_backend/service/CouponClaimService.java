package com.antojito.maps_backend.service;

import com.antojito.maps_backend.dto.ClaimedCouponResponse;
import com.antojito.maps_backend.exception.ResourceNotFoundException;
import com.antojito.maps_backend.model.ClaimedCoupon;
import com.antojito.maps_backend.model.ClaimedCouponStatus;
import com.antojito.maps_backend.model.Coupon;
import com.antojito.maps_backend.model.CouponStatus;
import com.antojito.maps_backend.repository.ClaimedCouponRepository;
import com.antojito.maps_backend.repository.ClientRepository;
import com.antojito.maps_backend.repository.CouponRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    private String generateClaimCode() {
        String claimCode;
        do {
            claimCode = CLAIM_CODE_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        } while (claimedCouponRepository.findByClaimCode(claimCode).isPresent());
        return claimCode;
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
