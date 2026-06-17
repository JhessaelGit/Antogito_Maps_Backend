package com.antojito.maps_backend.repository;

import com.antojito.maps_backend.model.LoyaltyAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, UUID> {

    Optional<LoyaltyAccount> findByClient_Uuid(UUID clientUuid);

    @Query("select count(a) from LoyaltyAccount a where a.accumulatedPoints > 0")
    long countClientsWithPoints();

    long countByCurrentLevel(String currentLevel);
}
