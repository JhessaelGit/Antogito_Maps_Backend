package com.antojito.maps_backend.dto;

import com.antojito.maps_backend.model.ClaimedCouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta de cupon reclamado")
public class ClaimedCouponResponse {

    private UUID uuid;
    private UUID couponId;
    private UUID restaurantId;
    private UUID clientId;
    private String claimCode;
    private ClaimedCouponStatus status;
    private String couponName;
    private String couponDescription;
    private LocalDate expirationDate;
    private LocalDateTime claimedAt;
    private LocalDateTime usedAt;
}
