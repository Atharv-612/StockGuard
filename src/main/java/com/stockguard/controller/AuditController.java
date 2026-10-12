package com.stockguard.controller;

import com.stockguard.entity.AuditLog;
import com.stockguard.entity.Shop;
import com.stockguard.service.AuthService;
import com.stockguard.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;
    private final AuthService authService;

    public AuditController(AuditService auditService, AuthService authService) {
        this.auditService = auditService;
        this.authService = authService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'OWNER')")
    public ResponseEntity<List<AuditLog>> getShopAuditLogs() {
        Shop shop = authService.getCurrentUserShop();
        return ResponseEntity.ok(auditService.getRecentLogsForShop(shop.getId()));
    }
}
