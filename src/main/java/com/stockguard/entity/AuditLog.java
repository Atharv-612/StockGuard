package com.stockguard.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_shop_time", columnList = "shop_id, created_at"),
        @Index(name = "idx_audit_action", columnList = "action")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id")
    private Long shopId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_email", length = 120)
    private String userEmail;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 50)
    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(length = 30)
    private String status = "SUCCESS";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AuditLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
