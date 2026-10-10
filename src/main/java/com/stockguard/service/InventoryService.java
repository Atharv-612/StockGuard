package com.stockguard.service;

import com.stockguard.dto.ReportDto;
import com.stockguard.entity.Product;
import com.stockguard.entity.Shop;
import com.stockguard.entity.StockMovement;
import com.stockguard.exception.BusinessException;
import com.stockguard.exception.ResourceNotFoundException;
import com.stockguard.repository.ProductRepository;
import com.stockguard.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AuthService authService;

    public InventoryService(ProductRepository productRepository,
                            StockMovementRepository stockMovementRepository,
                            AuthService authService) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.authService = authService;
    }

    @Transactional
    public void adjustStock(Long productId, int quantityDifference, String reason) {
        Shop shop = authService.getCurrentUserShop();
        Product product = productRepository.findByIdAndShopId(productId, shop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        int current = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        int newStock = current + quantityDifference;
        if (newStock < 0) {
            throw new BusinessException("Adjustment would cause negative inventory. Current: " + current + ", diff: " + quantityDifference);
        }

        product.setCurrentStock(newStock);
        productRepository.save(product);

        StockMovement movement = new StockMovement();
        movement.setShop(shop);
        movement.setProduct(product);
        movement.setMovementType("ADJUSTMENT");
        movement.setQuantityChanged(quantityDifference);
        movement.setQuantityAfter(newStock);
        movement.setReference("ADJ-" + System.currentTimeMillis() % 10000);
        movement.setNotes(reason != null ? reason : "Manual inventory adjustment");
        stockMovementRepository.save(movement);
    }

    public List<ReportDto.StockMovementDto> getRecentMovements() {
        Shop shop = authService.getCurrentUserShop();
        return stockMovementRepository.findTop20ByShopIdOrderByCreatedAtDesc(shop.getId()).stream()
                .map(this::mapMovement)
                .collect(Collectors.toList());
    }

    private ReportDto.StockMovementDto mapMovement(StockMovement m) {
        ReportDto.StockMovementDto dto = new ReportDto.StockMovementDto();
        dto.setId(m.getId());
        dto.setProductName(m.getProduct().getName());
        dto.setBatchNumber(m.getBatch() != null ? m.getBatch().getBatchNumber() : "N/A");
        dto.setMovementType(m.getMovementType());
        dto.setQuantityChanged(m.getQuantityChanged());
        dto.setQuantityAfter(m.getQuantityAfter());
        dto.setReference(m.getReference());
        dto.setNotes(m.getNotes());
        dto.setCreatedAt(m.getCreatedAt());
        return dto;
    }
}
