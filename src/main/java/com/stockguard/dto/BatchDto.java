package com.stockguard.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class BatchDto {

    public static class BatchRequest {
        @NotNull(message = "Product ID is required")
        private Long productId;

        @NotBlank(message = "Batch number is required")
        private String batchNumber;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        @NotNull(message = "Cost price is required")
        @DecimalMin(value = "0.0", inclusive = true)
        private BigDecimal costPrice;

        @NotNull(message = "Selling price is required")
        @DecimalMin(value = "0.0", inclusive = true)
        private BigDecimal sellingPrice;

        private LocalDate mfgDate;

        @NotNull(message = "Expiry date is required")
        private LocalDate expiryDate;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getCostPrice() { return costPrice; }
        public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
        public BigDecimal getSellingPrice() { return sellingPrice; }
        public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
        public LocalDate getMfgDate() { return mfgDate; }
        public void setMfgDate(LocalDate mfgDate) { this.mfgDate = mfgDate; }
        public LocalDate getExpiryDate() { return expiryDate; }
        public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    }

    public static class BatchResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String categoryName;
        private String batchNumber;
        private Integer initialQuantity;
        private Integer currentQuantity;
        private BigDecimal costPrice;
        private BigDecimal sellingPrice;
        private LocalDate mfgDate;
        private LocalDate expiryDate;
        private String status;
        private long daysUntilExpiry;
        private String expiryState; // SAFE, EXPIRING_SOON, EXPIRED
        private LocalDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
        public Integer getInitialQuantity() { return initialQuantity; }
        public void setInitialQuantity(Integer initialQuantity) { this.initialQuantity = initialQuantity; }
        public Integer getCurrentQuantity() { return currentQuantity; }
        public void setCurrentQuantity(Integer currentQuantity) { this.currentQuantity = currentQuantity; }
        public BigDecimal getCostPrice() { return costPrice; }
        public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
        public BigDecimal getSellingPrice() { return sellingPrice; }
        public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
        public LocalDate getMfgDate() { return mfgDate; }
        public void setMfgDate(LocalDate mfgDate) { this.mfgDate = mfgDate; }
        public LocalDate getExpiryDate() { return expiryDate; }
        public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getDaysUntilExpiry() { return daysUntilExpiry; }
        public void setDaysUntilExpiry(long daysUntilExpiry) { this.daysUntilExpiry = daysUntilExpiry; }
        public String getExpiryState() { return expiryState; }
        public void setExpiryState(String expiryState) { this.expiryState = expiryState; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
