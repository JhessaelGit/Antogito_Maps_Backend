package com.antojito.maps_backend.repository;

import com.antojito.maps_backend.model.Admin;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<Admin, UUID> {

    Optional<Admin> findByMail(String mail);

    Optional<Admin> findByMailAndIsDeletedFalse(String mail);

    Optional<Admin> findByUuidAndIsDeletedFalse(UUID uuid);

    List<Admin> findByIsDeletedFalseOrderByMailAsc();

    List<Admin> findByIsDeletedTrueOrderByDeletedAtDesc();

    long countByIsDeletedFalse();

    boolean existsByMailAndIsDeletedFalse(String mail);

    boolean existsByMailAndUuidNotAndIsDeletedFalse(String mail, UUID uuid);
}
