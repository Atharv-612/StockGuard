package com.stockguard.service;

import com.stockguard.entity.Product;
import com.stockguard.entity.ProductBatch;
import com.stockguard.entity.Shop;
import com.stockguard.repository.ProductBatchRepository;
import com.stockguard.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private final ProductRepository productRepository;
    private final ProductBatchRepository batchRepository;
    private final BatchService batchService;
    private final ProductService productService;
    private final AuthService authService;

    public AlertService(ProductRepository productRepository,
                        ProductBatchRepository batchRepository,
                        BatchService batchService,
                        ProductService productService,
                        AuthService authService) {
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.batchService = batchService;
        this.productService = productService;
        this.authService = authService;
    }

    public Map<String, Object> getAllActiveAlerts() {
        Shop shop = authService.getCurrentUserShop();
        LocalDate today = LocalDate.now();
        int warningDays = shop.getExpiryWarningDays() != null ? shop.getExpiryWarningDays() : 7;
        LocalDate warningDate = today.plusDays(warningDays);

        List<Product> lowStock = productRepository.findLowStockProducts(shop.getId());
        List<ProductBatch> expiringBatches = batchRepository.findExpiringSoonBatches(shop.getId(), today, warningDate);
        List<ProductBatch> expiredBatches = batchRepository.findExpiredBatches(shop.getId(), today);

        Map<String, Object> result = new HashMap<>();
        result.put("lowStockCount", lowStock.size());
        result.put("lowStockItems", lowStock.stream().map(productService::mapToResponse).collect(Collectors.toList()));

        result.put("expiringSoonCount", expiringBatches.size());
        result.put("expiringSoonItems", expiringBatches.stream().map(b -> batchService.mapToResponse(b, warningDays)).collect(Collectors.toList()));

        result.put("expiredCount", expiredBatches.size());
        result.put("expiredItems", expiredBatches.stream().map(b -> batchService.mapToResponse(b, warningDays)).collect(Collectors.toList()));

        return result;
    }
}
