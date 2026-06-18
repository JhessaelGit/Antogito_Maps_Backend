package com.antojito.maps_backend.repository;

import com.antojito.maps_backend.model.Coupon;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    List<Coupon> findByRestaurantIdOrderByCreatedAtDesc(UUID restaurantId);
}
