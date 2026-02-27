package com.edpp.identity.repository;

import com.edpp.identity.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
        List<AuditLog> findByUserIdOrderByTimestampDesc(String userId);
        List<AuditLog> findByEventTypeAndTimestampBetween(String eventType,
        LocalDateTime start,
        LocalDateTime end);
        List<AuditLog> findByResourceIdAndResource(String resourceId, String resource);
        }
