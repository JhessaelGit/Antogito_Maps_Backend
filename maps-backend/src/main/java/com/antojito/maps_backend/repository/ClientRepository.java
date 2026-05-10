package com.antojito.maps_backend.repository;

import com.antojito.maps_backend.model.Client;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {
    Optional<Client> findByMail(String mail);
    boolean existsByMail(String mail);
}
