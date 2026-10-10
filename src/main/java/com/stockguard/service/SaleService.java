package com.stockguard.service;

import com.stockguard.dto.SaleDto;
import com.stockguard.entity.*;
import com.stockguard.exception.BusinessException;
import com.stockguard.exception.ResourceNotFoundException;
import com.stockguard.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final ProductBatchRepository batchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AuthService authService;

    public SaleService(SaleRepository saleRepository,
                       SaleItemRepository saleItemRepository,
                       ProductRepository productRepository,
                       ProductBatchRepository batchRepository,
                       StockMovementRepository stockMovementRepository,
                       AuthService authService) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.authService = authService;
    }

    public List<SaleDto.SaleResponse> getAllSales() {
        Shop shop = authService.getCurrentUserShop();
        return saleRepository.findByShopIdOrderBySaleDateDesc(shop.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public SaleDto.SaleResponse getSaleById(Long id) {
        Shop shop = authService.getCurrentUserShop();
        Sale sale = saleRepository.findByIdAndShopId(id, shop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + id));
        return mapToResponse(sale);
    }

    @Transactional
    public SaleDto.SaleResponse processSale(SaleDto.SaleRequest request) {
        Shop shop = authService.getCurrentUserShop();
        User user = authService.getCurrentAuthenticatedUser();

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Sale must have at least one product line item");
        }

        Sale sale = new Sale();
        sale.setShop(shop);
        sale.setUser(user);
        sale.setInvoiceNumber("INV-" + System.currentTimeMillis());
        sale.setCustomerName(request.getCustomerName() != null && !request.getCustomerName().trim().isEmpty() ? request.getCustomerName() : "Walk-in Customer");
        sale.setCustomerPhone(request.getCustomerPhone());
        sale.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH");
        sale.setSaleDate(LocalDateTime.now());

        BigDecimal subtotal = BigDecimal.ZERO;
        List<SaleItem> saleItems = new ArrayList<>();

        for (SaleDto.SaleItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() <= 0) {
                throw new BusinessException("Sale item quantity must be greater than zero");
            }

            Product product = productRepository.findByIdAndShopId(itemReq.getProductId(), shop.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemReq.getProductId()));

            int reqQty = itemReq.getQuantity();
            int currentStock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;

            if (currentStock < reqQty) {
                throw new BusinessException("Insufficient stock for product '" + product.getName() + "'. Available: " + currentStock + ", Requested: " + reqQty);
            }

            // Deduct from batches
            int remainingToDeduct = reqQty;
            List<ProductBatch> availableBatches;

            if (itemReq.getBatchId() != null) {
                ProductBatch specificBatch = batchRepository.findByIdAndProductShopId(itemReq.getBatchId(), shop.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + itemReq.getBatchId()));
                if (specificBatch.getCurrentQuantity() < reqQty) {
                    throw new BusinessException("Selected batch " + specificBatch.getBatchNumber() + " only has " + specificBatch.getCurrentQuantity() + " units available.");
                }
                availableBatches = List.of(specificBatch);
            } else {
                // FIFO batch allocation (earliest expiry first)
                availableBatches = batchRepository.findAvailableBatchesForSale(product.getId());
            }

            // Decrement batches
            ProductBatch assignedBatch = null;
            for (ProductBatch batch : availableBatches) {
                if (remainingToDeduct <= 0) break;

                // Check expiry: never sell expired products
                if (batch.getExpiryDate().isBefore(LocalDate.now())) {
                    continue; // Skip expired batches from auto-sale
                }

                int deduct = Math.min(batch.getCurrentQuantity(), remainingToDeduct);
                batch.setCurrentQuantity(batch.getCurrentQuantity() - deduct);
                batchRepository.save(batch);
                remainingToDeduct -= deduct;
                assignedBatch = batch;
            }

            // Update product stock
            int newProductStock = currentStock - reqQty;
            product.setCurrentStock(newProductStock);
            productRepository.save(product);

            // Calculate line total
            BigDecimal unitPrice = product.getSellingPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(reqQty));
            subtotal = subtotal.add(lineTotal);

            // Create SaleItem
            SaleItem saleItem = new SaleItem();
            saleItem.setSale(sale);
            saleItem.setProduct(product);
            saleItem.setBatch(assignedBatch);
            saleItem.setProductName(product.getName());
            saleItem.setQuantity(reqQty);
            saleItem.setUnitPrice(unitPrice);
            saleItem.setTotalPrice(lineTotal);
            saleItems.add(saleItem);

            // Record Stock Movement
            StockMovement movement = new StockMovement();
            movement.setShop(shop);
            movement.setProduct(product);
            movement.setBatch(assignedBatch);
            movement.setMovementType("SALE");
            movement.setQuantityChanged(-reqQty);
            movement.setQuantityAfter(newProductStock);
            movement.setReference(sale.getInvoiceNumber());
            movement.setNotes("Sale to " + sale.getCustomerName());
            stockMovementRepository.save(movement);
        }

        sale.setSubtotal(subtotal);
        BigDecimal discount = request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO;
        sale.setDiscount(discount);
        BigDecimal finalTotal = subtotal.subtract(discount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) finalTotal = BigDecimal.ZERO;
        sale.setTotalAmount(finalTotal);
        sale.setItems(saleItems);

        Sale savedSale = saleRepository.save(sale);
        return mapToResponse(savedSale);
    }

    public SaleDto.SaleResponse mapToResponse(Sale sale) {
        SaleDto.SaleResponse resp = new SaleDto.SaleResponse();
        resp.setId(sale.getId());
        resp.setInvoiceNumber(sale.getInvoiceNumber());
        resp.setCustomerName(sale.getCustomerName());
        resp.setCustomerPhone(sale.getCustomerPhone());
        resp.setSubtotal(sale.getSubtotal());
        resp.setTax(sale.getTax());
        resp.setDiscount(sale.getDiscount());
        resp.setTotalAmount(sale.getTotalAmount());
        resp.setPaymentMethod(sale.getPaymentMethod());
        resp.setSaleDate(sale.getSaleDate());

        if (sale.getItems() != null) {
            resp.setItems(sale.getItems().stream().map(item -> {
                SaleDto.SaleItemResponse itemResp = new SaleDto.SaleItemResponse();
                itemResp.setId(item.getId());
                itemResp.setProductId(item.getProduct().getId());
                itemResp.setProductName(item.getProductName());
                if (item.getBatch() != null) {
                    itemResp.setBatchId(item.getBatch().getId());
                    itemResp.setBatchNumber(item.getBatch().getBatchNumber());
                }
                itemResp.setQuantity(item.getQuantity());
                itemResp.setUnitPrice(item.getUnitPrice());
                itemResp.setTotalPrice(item.getTotalPrice());
                return itemResp;
            }).collect(Collectors.toList()));
        }

        return resp;
    }
}
