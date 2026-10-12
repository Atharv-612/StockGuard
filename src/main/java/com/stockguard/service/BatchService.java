package com.stockguard.service;

import com.stockguard.dto.BatchDto;
import com.stockguard.entity.Product;
import com.stockguard.entity.ProductBatch;
import com.stockguard.entity.Shop;
import com.stockguard.entity.StockMovement;
import com.stockguard.exception.BusinessException;
import com.stockguard.exception.ResourceNotFoundException;
import com.stockguard.repository.ProductBatchRepository;
import com.stockguard.repository.ProductRepository;
import com.stockguard.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BatchService {

    private final ProductBatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AuthService authService;
    private final AuditService auditService;

    public BatchService(ProductBatchRepository batchRepository,
                        ProductRepository productRepository,
                        StockMovementRepository stockMovementRepository,
                        AuthService authService,
                        AuditService auditService) {
        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.authService = authService;
        this.auditService = auditService;
    }

    public List<BatchDto.BatchResponse> getAllBatches(String filter) {
        Shop shop = authService.getCurrentUserShop();
        LocalDate today = LocalDate.now();
        int warningDays = shop.getExpiryWarningDays() != null ? shop.getExpiryWarningDays() : 7;
        LocalDate warningDate = today.plusDays(warningDays);

        List<ProductBatch> batches;
        if ("expired".equalsIgnoreCase(filter)) {
            batches = batchRepository.findExpiredBatches(shop.getId(), today);
        } else if ("expiring".equalsIgnoreCase(filter)) {
            batches = batchRepository.findExpiringSoonBatches(shop.getId(), today, warningDate);
        } else {
            batches = batchRepository.findAllByShopId(shop.getId());
        }

        return batches.stream()
                .map(b -> mapToResponse(b, warningDays))
                .collect(Collectors.toList());
    }

    public List<BatchDto.BatchResponse> getBatchesByProduct(Long productId) {
        Shop shop = authService.getCurrentUserShop();
        Product product = productRepository.findByIdAndShopId(productId, shop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        int warningDays = shop.getExpiryWarningDays() != null ? shop.getExpiryWarningDays() : 7;
        return batchRepository.findByProductIdOrderByExpiryDateAsc(productId).stream()
                .map(b -> mapToResponse(b, warningDays))
                .collect(Collectors.toList());
    }

    @Transactional
    public BatchDto.BatchResponse createBatch(BatchDto.BatchRequest request) {
        Shop shop = authService.getCurrentUserShop();
        Product product = productRepository.findByIdAndShopId(request.getProductId(), shop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (request.getQuantity() <= 0) {
            throw new BusinessException("Batch quantity must be greater than zero");
        }

        ProductBatch batch = new ProductBatch();
        batch.setProduct(product);
        batch.setBatchNumber(request.getBatchNumber());
        batch.setInitialQuantity(request.getQuantity());
        batch.setCurrentQuantity(request.getQuantity());
        batch.setCostPrice(request.getCostPrice());
        batch.setSellingPrice(request.getSellingPrice());
        batch.setMfgDate(request.getMfgDate());
        batch.setExpiryDate(request.getExpiryDate());
        batch.setStatus("ACTIVE");
        ProductBatch savedBatch = batchRepository.save(batch);

        // Update total product stock
        int updatedStock = (product.getCurrentStock() != null ? product.getCurrentStock() : 0) + request.getQuantity();
        product.setCurrentStock(updatedStock);
        productRepository.save(product);

        // Record stock movement
        StockMovement movement = new StockMovement();
        movement.setShop(shop);
        movement.setProduct(product);
        movement.setBatch(savedBatch);
        movement.setMovementType("PURCHASE");
        movement.setQuantityChanged(request.getQuantity());
        movement.setQuantityAfter(updatedStock);
        movement.setReference("BATCH-" + savedBatch.getBatchNumber());
        movement.setNotes("New inventory batch received");
        stockMovementRepository.save(movement);

        var currentUser = authService.getCurrentAuthenticatedUser();
        auditService.logEvent(shop.getId(), currentUser.getId(), currentUser.getEmail(),
                "BATCH_CREATED", "BATCH", savedBatch.getId().toString(),
                "Created batch '" + savedBatch.getBatchNumber() + "' for product " + product.getName() + " (qty: " + request.getQuantity() + ")", null, "SUCCESS");

        int warningDays = shop.getExpiryWarningDays() != null ? shop.getExpiryWarningDays() : 7;
        return mapToResponse(savedBatch, warningDays);
    }

    @Transactional
    public void discardExpiredBatch(Long batchId, String reason) {
        Shop shop = authService.getCurrentUserShop();
        var currentUser = authService.getCurrentAuthenticatedUser();
        ProductBatch batch = batchRepository.findByIdAndProductShopId(batchId, shop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + batchId));

        int quantityToRemove = batch.getCurrentQuantity();
        if (quantityToRemove <= 0) {
            throw new BusinessException("Batch current quantity is already zero");
        }

        Product product = batch.getProduct();
        int newProductStock = Math.max(0, (product.getCurrentStock() != null ? product.getCurrentStock() : 0) - quantityToRemove);
        product.setCurrentStock(newProductStock);
        productRepository.save(product);

        batch.setCurrentQuantity(0);
        batch.setStatus("DISCARDED");
        batchRepository.save(batch);

        // Record discard movement
        StockMovement movement = new StockMovement();
        movement.setShop(shop);
        movement.setProduct(product);
        movement.setBatch(batch);
        movement.setMovementType("EXPIRED_SCRAP");
        movement.setQuantityChanged(-quantityToRemove);
        movement.setQuantityAfter(newProductStock);
        movement.setReference("SCRAP-" + batch.getBatchNumber());
        movement.setNotes(reason != null ? reason : "Expired stock discarded");
        stockMovementRepository.save(movement);

        auditService.logEvent(shop.getId(), currentUser.getId(), currentUser.getEmail(),
                "BATCH_DISCARDED", "BATCH", batchId.toString(),
                "Discarded expired batch '" + batch.getBatchNumber() + "' (qty: " + quantityToRemove + "). Reason: " + reason, null, "SUCCESS");
    }

    public BatchDto.BatchResponse mapToResponse(ProductBatch batch, int warningDays) {
        BatchDto.BatchResponse resp = new BatchDto.BatchResponse();
        resp.setId(batch.getId());
        resp.setProductId(batch.getProduct().getId());
        resp.setProductName(batch.getProduct().getName());
        if (batch.getProduct().getCategory() != null) {
            resp.setCategoryName(batch.getProduct().getCategory().getName());
        }
        resp.setBatchNumber(batch.getBatchNumber());
        resp.setInitialQuantity(batch.getInitialQuantity());
        resp.setCurrentQuantity(batch.getCurrentQuantity());
        resp.setCostPrice(batch.getCostPrice());
        resp.setSellingPrice(batch.getSellingPrice());
        resp.setMfgDate(batch.getMfgDate());
        resp.setExpiryDate(batch.getExpiryDate());
        resp.setStatus(batch.getStatus());
        resp.setCreatedAt(batch.getCreatedAt());

        LocalDate today = LocalDate.now();
        long daysUntil = ChronoUnit.DAYS.between(today, batch.getExpiryDate());
        resp.setDaysUntilExpiry(daysUntil);

        // Calculate strict status: Never mark expired as safe!
        if (daysUntil < 0) {
            resp.setExpiryState("EXPIRED");
        } else if (daysUntil <= warningDays) {
            resp.setExpiryState("EXPIRING_SOON");
        } else {
            resp.setExpiryState("SAFE");
        }

        return resp;
    }
}
