package com.stockguard.repository;

import com.stockguard.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop50ByShopIdOrderByCreatedAtDesc(Long shopId);

    Page<AuditLog> findByShopIdOrderByCreatedAtDesc(Long shopId, Pageable pageable);

    List<AuditLog> findTop100ByEventTypeOrderByCreatedAtDesc(AuditLog.EventType eventType);

    @Query("SELECT a FROM AuditLog a WHERE a.userEmail = :email ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentByUserEmail(@Param("email") String email, Pageable pageable);

    /** Count login failures for a given email within a time window (brute-force detection) */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.userEmail = :email " +
           "AND a.eventType = com.stockguard.entity.AuditLog.EventType.LOGIN_FAILURE " +
           "AND a.createdAt >= :since")
    long countLoginFailuresSince(@Param("email") String email, @Param("since") LocalDateTime since);

    /** Count login failures from an IP within a time window */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.ipAddress = :ip " +
           "AND a.eventType = com.stockguard.entity.AuditLog.EventType.LOGIN_FAILURE " +
           "AND a.createdAt >= :since")
    long countLoginFailuresByIpSince(@Param("ip") String ip, @Param("since") LocalDateTime since);

    @Modifying
    @Transactional
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
