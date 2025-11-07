package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.AuditLog;
import com.buildbetter.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    Page<AuditLog> findByUserId(String userId, Pageable pageable);
    Page<AuditLog> findByOrganizationId(String organizationId, Pageable pageable);
    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);
    Page<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.organization.id = :organizationId AND al.timestamp >= :since")
    List<AuditLog> findRecentByOrganization(String organizationId, LocalDateTime since);

    @Query("SELECT al FROM AuditLog al WHERE al.user.id = :userId AND al.action IN :actions")
    List<AuditLog> findByUserIdAndActions(String userId, List<AuditAction> actions);
}
