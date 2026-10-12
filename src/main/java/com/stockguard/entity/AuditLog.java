package com.stockguard.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Persistent audit log for security-relevant and business-critical events.
 * Stores WHO did WHAT on WHICH resource, from WHERE, and WHEN.
 * Never stores passwords, tokens, or other secrets.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_shop", columnList = "shop_id"),
        @Index(name = "idx_audit_user", columnList = "user_email"),
        @Index(name = "idx_audit_event", columnList = "event_type"),
        @Index(name = "idx_audit_created", columnList = "created_at")
})
public class AuditLog {

    /** Severity / category of the event */
    public enum EventType {
        // Authentication events
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGOUT,
        REGISTER_SUCCESS,
        TOKEN_REFRESH,

        // Data-mutation events
        PRODUCT_CREATED,
        PRODUCT_UPDATED,
        PRODUCT_DELETED,
        BATCH_CREATED,
        BATCH_DISCARDED_EXPIRED,
        STOCK_ADJUSTED,
        SALE_PROCESSED,

        // Admin/Settings events
        SHOP_SETTINGS_UPDATED,
        NOTIFICATION_PREF_UPDATED,
        WHATSAPP_SENT,

        // Security events
        ACCESS_DENIED,
        INVALID_TOKEN,
        SUSPICIOUS_REQUEST
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nullable: null for pre-auth events (e.g. LOGIN_FAILURE before user is found) */
    @Column(name = "user_email", length = 120)
    private String userEmail;

    /** Nullable: null when shop context is not yet established */
    @Column(name = "shop_id")
    private Long shopId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    /** Human-readable description - MUST NOT contain passwords or tokens */
    @Column(nullable = false, length = 500)
    private String description;

    /** The resource type acted upon (e.g. "Product", "Sale", "Batch") */
    @Column(name = "resource_type", length = 50)
    private String resourceType;

    /** The ID of the resource acted upon */
    @Column(name = "resource_id")
    private Long resourceId;

    /** Client IP address for security events */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** HTTP method + path, e.g. "DELETE /api/products/5" */
    @Column(name = "request_path", length = 255)
    private String requestPath;

    /** Outcome: SUCCESS, FAILURE, BLOCKED */
    @Column(length = 20)
    private String outcome = "SUCCESS";

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AuditLog() {}

    public Long getId() { return id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getRequestPath() { return requestPath; }
    public void setRequestPath(String requestPath) { this.requestPath = requestPath; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
