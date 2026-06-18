package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Cupon usado dentro del ranking del dashboard")
public class DashboardTopCouponResponse {

    private UUID couponId;
    private String couponName;
    private Long totalUsed;
}
