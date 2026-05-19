package com.antojito.maps_backend.repository;

import com.antojito.maps_backend.model.Restaurante;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestauranteRepository extends JpaRepository<Restaurante, UUID> {

    Optional<Restaurante> findByName(String name);

    List<Restaurante> findByIsBlockedFalse();
}
