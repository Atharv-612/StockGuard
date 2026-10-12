package com.stockguard.controller;

import com.stockguard.entity.Shop;
import com.stockguard.repository.ShopRepository;
import com.stockguard.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingController {

    private final AuthService authService;
    private final ShopRepository shopRepository;
    private final javax.sql.DataSource dataSource;
    private final com.stockguard.service.AuditService auditService;

    public SettingController(AuthService authService,
                             ShopRepository shopRepository,
                             javax.sql.DataSource dataSource,
                             com.stockguard.service.AuditService auditService) {
        this.authService = authService;
        this.shopRepository = shopRepository;
        this.dataSource = dataSource;
        this.auditService = auditService;
    }

    @GetMapping("/db-status")
    public ResponseEntity<Map<String, Object>> getDbStatus() {
        Map<String, Object> status = new java.util.HashMap<>();
        try (java.sql.Connection conn = dataSource.getConnection()) {
            java.sql.DatabaseMetaData meta = conn.getMetaData();
            String prodName = meta.getDatabaseProductName();
            boolean isH2 = prodName != null && prodName.toLowerCase().contains("h2");
            status.put("connected", true);
            status.put("isH2", isH2);
            status.put("engineName", isH2 ? "Local File Database (stockguard_data.mv.db)" : "MySQL 8.0 Database");
            status.put("status", "HEALTHY");
            status.put("message", isH2
                    ? "Active & healthy. All data is persistently saved in stockguard_data.mv.db. To use MySQL, set DB_PASSWORD in .env or application.properties."
                    : "Active & healthy. Connected directly to MySQL database.");
        } catch (Exception e) {
            status.put("connected", false);
            status.put("status", "ERROR");
            status.put("message", "Database connection currently unavailable");
        }
        return ResponseEntity.ok(status);
    }

    @GetMapping("/shop")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SHOP_OWNER', 'OWNER', 'MANAGER')")
    public ResponseEntity<Shop> getShopSettings() {
        return ResponseEntity.ok(authService.getCurrentUserShop());
    }

    @PutMapping("/shop")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SHOP_OWNER', 'OWNER')")
    public ResponseEntity<Shop> updateShopSettings(@RequestBody Map<String, Object> payload) {
        Shop shop = authService.getCurrentUserShop();
        var user = authService.getCurrentAuthenticatedUser();

        if (payload.containsKey("name")) shop.setName(payload.get("name").toString());
        if (payload.containsKey("ownerName")) shop.setOwnerName(payload.get("ownerName").toString());
        if (payload.containsKey("phone")) shop.setPhone(payload.get("phone").toString());
        if (payload.containsKey("email")) shop.setEmail(payload.get("email").toString());
        if (payload.containsKey("address")) shop.setAddress(payload.get("address").toString());
        if (payload.containsKey("currency")) shop.setCurrency(payload.get("currency").toString());
        if (payload.containsKey("minStockDefault")) {
            shop.setMinStockDefault(Integer.parseInt(payload.get("minStockDefault").toString()));
        }
        if (payload.containsKey("expiryWarningDays")) {
            shop.setExpiryWarningDays(Integer.parseInt(payload.get("expiryWarningDays").toString()));
        }

        Shop updated = shopRepository.save(shop);
        auditService.logEvent(shop.getId(), user.getId(), user.getEmail(),
                "SETTINGS_UPDATED", "SHOP", shop.getId().toString(),
                "Updated shop settings for " + shop.getName(), null, "SUCCESS");
        return ResponseEntity.ok(updated);
    }
}
