package com.antojito.maps_backend.repository;

import com.antojito.maps_backend.model.ClaimedCoupon;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimedCouponRepository extends JpaRepository<ClaimedCoupon, UUID> {

    long countByCouponId(UUID couponId);

    boolean existsByCouponIdAndClientId(UUID couponId, UUID clientId);

    Optional<ClaimedCoupon> findByClaimCode(String claimCode);

    List<ClaimedCoupon> findByClientIdOrderByClaimedAtDesc(UUID clientId);
}
