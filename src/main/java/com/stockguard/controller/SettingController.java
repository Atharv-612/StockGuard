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

    public SettingController(AuthService authService, ShopRepository shopRepository, javax.sql.DataSource dataSource) {
        this.authService = authService;
        this.shopRepository = shopRepository;
        this.dataSource = dataSource;
    }

    @GetMapping("/db-status")
    public ResponseEntity<Map<String, Object>> getDbStatus() {
        Map<String, Object> status = new java.util.HashMap<>();
        try (java.sql.Connection conn = dataSource.getConnection()) {
            java.sql.DatabaseMetaData meta = conn.getMetaData();
            String prodName = meta.getDatabaseProductName();
            String prodVersion = meta.getDatabaseProductVersion();
            status.put("connected", true);
            status.put("databaseProduct", prodName);
            status.put("databaseVersion", prodVersion);
            status.put("databaseUrl", meta.getURL());
            status.put("shopsCount", shopRepository.count());
            boolean isH2 = prodName.toLowerCase().contains("h2");
            status.put("isH2", isH2);
            status.put("engineName", isH2 ? "Local File Database (stockguard_data.mv.db)" : "MySQL 8.0 Database");
            status.put("status", "HEALTHY");
            status.put("message", isH2
                    ? "Active & healthy. All data is persistently saved in stockguard_data.mv.db. To use MySQL, set DB_PASSWORD in .env or application.properties."
                    : "Active & healthy. Connected directly to MySQL database.");
        } catch (Exception e) {
            status.put("connected", false);
            status.put("status", "ERROR");
            status.put("message", e.getMessage());
        }
        return ResponseEntity.ok(status);
    }

    @GetMapping("/shop")
    public ResponseEntity<Shop> getShopSettings() {
        return ResponseEntity.ok(authService.getCurrentUserShop());
    }

    @PutMapping("/shop")
    public ResponseEntity<Shop> updateShopSettings(@RequestBody Map<String, Object> payload) {
        Shop shop = authService.getCurrentUserShop();

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
        return ResponseEntity.ok(updated);
    }
}
