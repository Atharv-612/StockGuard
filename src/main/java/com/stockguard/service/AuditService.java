package com.stockguard.service;

import com.stockguard.entity.AuditLog;
import com.stockguard.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(password|token|secret|authorization|bearer|key|credentials)[\"':= ]+([^,\"&;\\s]+)"
    );

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(Long shopId, Long userId, String userEmail, String action,
                         String entityType, String entityId, String details,
                         String ipAddress, String status) {
        try {
            AuditLog audit = new AuditLog();
            audit.setShopId(shopId);
            audit.setUserId(userId);
            audit.setUserEmail(userEmail);
            audit.setAction(action);
            audit.setEntityType(entityType);
            audit.setEntityId(entityId);
            audit.setDetails(sanitizeDetails(details));
            audit.setIpAddress(ipAddress != null && ipAddress.length() > 60 ? ipAddress.substring(0, 60) : ipAddress);
            audit.setStatus(status != null ? status : "SUCCESS");
            audit.setCreatedAt(LocalDateTime.now());

            auditLogRepository.save(audit);
            log.info("SECURITY AUDIT [{}]: action={}, user={}, shop={}, ip={}, status={}",
                    action, action, userEmail, shopId, ipAddress, status);
        } catch (Exception ex) {
            log.error("Failed to write audit log entry: {}", ex.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSecurityEvent(String action, String userEmail, String details, String ipAddress, String status) {
        logEvent(null, null, userEmail, action, "SECURITY", null, details, ipAddress, status);
    }

    public List<AuditLog> getRecentLogsForShop(Long shopId) {
        return auditLogRepository.findTop100ByShopIdOrderByCreatedAtDesc(shopId);
    }

    /**
     * Defense-in-depth sanitization: scrub passwords, JWT tokens, secrets from audit entries.
     */
    public String sanitizeDetails(String details) {
        if (details == null) return null;
        return SENSITIVE_PATTERN.matcher(details).replaceAll("$1=[REDACTED]");
    }
}
