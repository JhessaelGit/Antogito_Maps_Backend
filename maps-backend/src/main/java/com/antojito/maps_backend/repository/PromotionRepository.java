package com.antojito.maps_backend.repository;

import com.antojito.maps_backend.model.Promotion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    List<Promotion> findByRestaurantIdAndIsActivePromotionTrueOrderByDateEndPromotionAsc(UUID restaurantId);
}
