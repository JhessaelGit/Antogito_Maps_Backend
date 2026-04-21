package com.antojito.maps_backend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Entidad de promociones por restaurante")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @Column(name = "id_restaurant", nullable = false)
    private UUID restaurantId;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "percent_discount", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentDiscount;

    @Column(name = "date_start_promotion", nullable = false)
    private LocalDate dateStartPromotion;

    @Column(name = "date_end_promotion", nullable = false)
    private LocalDate dateEndPromotion;

    @Column(name = "is_active_promotion", nullable = false)
    private Boolean isActivePromotion;

    @PrePersist
    public void applyDefaults() {
        if (isActivePromotion == null) {
            isActivePromotion = Boolean.TRUE;
        }
    }
}
