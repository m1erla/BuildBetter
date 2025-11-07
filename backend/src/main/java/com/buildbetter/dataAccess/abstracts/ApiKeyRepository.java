package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {
    Optional<ApiKey> findByKeyHash(String keyHash);
    List<ApiKey> findByOrganizationId(String organizationId);
    List<ApiKey> findByCreatedById(String userId);

    @Query("SELECT ak FROM ApiKey ak WHERE ak.organization.id = :organizationId AND ak.isActive = true AND (ak.expiresAt IS NULL OR ak.expiresAt > :now) AND ak.revokedAt IS NULL")
    List<ApiKey> findActiveByOrganization(String organizationId, LocalDateTime now);

    @Query("SELECT COUNT(ak) FROM ApiKey ak WHERE ak.organization.id = :organizationId AND ak.isActive = true")
    long countActiveByOrganization(String organizationId);
}
