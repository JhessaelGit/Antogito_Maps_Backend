package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
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
@Schema(description = "Metricas analiticas del restaurante")
public class DashboardResponse {

    private UUID restaurantId;
    private Long activeCoupons;
    private Long totalClaimedCoupons;
    private Long totalUsedCoupons;
    private Long recurringClients;
    private Long expiredPromotions;
    private List<DashboardTopCouponResponse> topUsedCoupons;
}
