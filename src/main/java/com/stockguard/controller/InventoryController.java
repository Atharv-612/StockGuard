package com.stockguard.controller;

import com.stockguard.dto.ReportDto;
import com.stockguard.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/adjust")
    public ResponseEntity<Map<String, String>> adjustStock(@RequestBody Map<String, Object> payload) {
        Long productId = Long.valueOf(payload.get("productId").toString());
        int diff = Integer.parseInt(payload.get("quantityDifference").toString());
        String reason = payload.get("reason") != null ? payload.get("reason").toString() : "Manual count adjustment";

        inventoryService.adjustStock(productId, diff, reason);
        return ResponseEntity.ok(Map.of("message", "Stock adjusted successfully"));
    }

    @GetMapping("/movements")
    public ResponseEntity<List<ReportDto.StockMovementDto>> getRecentMovements() {
        return ResponseEntity.ok(inventoryService.getRecentMovements());
    }
}
