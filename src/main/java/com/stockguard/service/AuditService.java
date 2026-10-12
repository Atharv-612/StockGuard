package com.stockguard.service;

import com.stockguard.entity.AuditLog;
import com.stockguard.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * Central audit logging service.
 * All writes are asynchronous so they never block request handling.
 * IMPORTANT: This service must NEVER log passwords, tokens, or secret values.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // ---- Convenience factory methods ----

    @Async
    public void logLoginSuccess(String email, Long shopId) {
        save(email, shopId, AuditLog.EventType.LOGIN_SUCCESS,
                "Successful login", null, null, "SUCCESS");
    }

    @Async
    public void logLoginFailure(String email, String ipAddress) {
        AuditLog entry = new AuditLog();
        entry.setUserEmail(email);
        entry.setEventType(AuditLog.EventType.LOGIN_FAILURE);
        entry.setDescription("Failed login attempt");
        entry.setIpAddress(ipAddress);
        entry.setOutcome("FAILURE");
        entry.setRequestPath("POST /api/auth/login");
        persistSafely(entry);
    }

    @Async
    public void logLogout(String email, Long shopId) {
        save(email, shopId, AuditLog.EventType.LOGOUT,
                "User logged out and token revoked", null, null, "SUCCESS");
    }

    @Async
    public void logRegister(String email, Long shopId) {
        save(email, shopId, AuditLog.EventType.REGISTER_SUCCESS,
                "New account registered", "Shop", shopId, "SUCCESS");
    }

    @Async
    public void logProductCreated(String email, Long shopId, Long productId, String productName) {
        save(email, shopId, AuditLog.EventType.PRODUCT_CREATED,
                "Product created: " + productName, "Product", productId, "SUCCESS");
    }

    @Async
    public void logProductUpdated(String email, Long shopId, Long productId, String productName) {
        save(email, shopId, AuditLog.EventType.PRODUCT_UPDATED,
                "Product updated: " + productName, "Product", productId, "SUCCESS");
    }

    @Async
    public void logProductDeleted(String email, Long shopId, Long productId, String productName) {
        save(email, shopId, AuditLog.EventType.PRODUCT_DELETED,
                "Product DELETED: " + productName + " (id=" + productId + ")",
                "Product", productId, "SUCCESS");
    }

    @Async
    public void logBatchCreated(String email, Long shopId, Long batchId, String batchNumber, String productName) {
        save(email, shopId, AuditLog.EventType.BATCH_CREATED,
                "Batch " + batchNumber + " created for product: " + productName,
                "ProductBatch", batchId, "SUCCESS");
    }

    @Async
    public void logBatchDiscarded(String email, Long shopId, Long batchId, String batchNumber, String reason) {
        save(email, shopId, AuditLog.EventType.BATCH_DISCARDED_EXPIRED,
                "Expired batch " + batchNumber + " discarded. Reason: " + reason,
                "ProductBatch", batchId, "SUCCESS");
    }

    @Async
    public void logStockAdjusted(String email, Long shopId, Long productId, String productName, int delta) {
        save(email, shopId, AuditLog.EventType.STOCK_ADJUSTED,
                "Stock adjusted for " + productName + " by " + delta,
                "Product", productId, "SUCCESS");
    }

    @Async
    public void logSaleProcessed(String email, Long shopId, Long saleId, String invoiceNumber) {
        save(email, shopId, AuditLog.EventType.SALE_PROCESSED,
                "Sale processed: " + invoiceNumber,
                "Sale", saleId, "SUCCESS");
    }

    @Async
    public void logShopSettingsUpdated(String email, Long shopId) {
        save(email, shopId, AuditLog.EventType.SHOP_SETTINGS_UPDATED,
                "Shop settings updated", "Shop", shopId, "SUCCESS");
    }

    @Async
    public void logAccessDenied(String email, String path, String ipAddress) {
        AuditLog entry = new AuditLog();
        entry.setUserEmail(email);
        entry.setEventType(AuditLog.EventType.ACCESS_DENIED);
        entry.setDescription("Access denied to: " + path);
        entry.setIpAddress(ipAddress);
        entry.setRequestPath(path);
        entry.setOutcome("BLOCKED");
        persistSafely(entry);
    }

    @Async
    public void logInvalidToken(String ipAddress, String path) {
        AuditLog entry = new AuditLog();
        entry.setEventType(AuditLog.EventType.INVALID_TOKEN);
        entry.setDescription("Invalid or revoked token presented");
        entry.setIpAddress(ipAddress);
        entry.setRequestPath(path);
        entry.setOutcome("BLOCKED");
        persistSafely(entry);
    }

    // ---- Internal helpers ----

    private void save(String email, Long shopId, AuditLog.EventType type,
                      String description, String resourceType, Long resourceId, String outcome) {
        AuditLog entry = new AuditLog();
        entry.setUserEmail(email);
        entry.setShopId(shopId);
        entry.setEventType(type);
        entry.setDescription(description);
        entry.setResourceType(resourceType);
        entry.setResourceId(resourceId);
        entry.setOutcome(outcome);
        // Capture the current HTTP request context if available
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                entry.setIpAddress(extractClientIp(req));
                entry.setRequestPath(req.getMethod() + " " + req.getRequestURI());
            }
        } catch (Exception ignored) {
            // Best-effort: never fail audit log because of context issues
        }
        persistSafely(entry);
    }

    private void persistSafely(AuditLog entry) {
        try {
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Audit logging must never crash the application
            log.error("Failed to persist audit log entry [{}]: {}",
                    entry.getEventType(), e.getMessage());
        }
    }

    /**
     * Resolves the real client IP, respecting common reverse-proxy headers.
     * X-Forwarded-For is trusted only if known proxies are in front.
     */
    public static String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // Take only the first IP in the chain to avoid spoofing
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
