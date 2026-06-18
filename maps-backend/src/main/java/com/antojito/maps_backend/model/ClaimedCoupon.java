package com.antojito.maps_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "claimed_coupons")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimedCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @Column(name = "coupon_uuid", nullable = false)
    private UUID couponId;

    @Column(name = "client_uuid", nullable = false)
    private UUID clientId;

    @Column(name = "claim_code", nullable = false, unique = true, length = 120)
    private String claimCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ClaimedCouponStatus status;

    @CreationTimestamp
    @Column(name = "claimed_at", nullable = false, updatable = false)
    private LocalDateTime claimedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @PrePersist
    public void applyDefaults() {
        if (status == null) {
            status = ClaimedCouponStatus.CLAIMED;
        }
    }
}
