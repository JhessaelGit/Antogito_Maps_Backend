package com.antojito.maps_backend.repository;

import com.antojito.maps_backend.model.PointsHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PointsHistoryRepository extends JpaRepository<PointsHistory, UUID> {

    @Query("select coalesce(sum(ph.points), 0) from PointsHistory ph")
    long sumAllPoints();

    List<PointsHistory> findByClient_UuidOrderByCreatedAtDesc(UUID clientUuid);
}
