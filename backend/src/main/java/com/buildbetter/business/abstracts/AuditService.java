package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.AuditLog;
import com.buildbetter.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditService {
    void log(AuditAction action, String userId, String organizationId, String entityType, String entityId, String details);
    void logSuccess(AuditAction action, String userId, String organizationId, String entityType, String entityId);
    void logFailure(AuditAction action, String userId, String organizationId, String errorMessage);

    Page<AuditLog> getAuditLogs(String organizationId, Pageable pageable);
    Page<AuditLog> getUserAuditLogs(String userId, Pageable pageable);
    Page<AuditLog> getAuditLogsByAction(AuditAction action, Pageable pageable);
}
