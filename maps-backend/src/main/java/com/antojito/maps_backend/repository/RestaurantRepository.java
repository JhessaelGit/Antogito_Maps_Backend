package com.antojito.maps_backend.repository;

import com.antojito.maps_backend.model.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurante, Long> {
}