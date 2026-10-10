package com.stockguard.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductDto {

    public static class ProductRequest {
        @NotBlank(message = "Product name is required")
        private String name;

        private Long categoryId;
        private String sku;
        private String barcode;
        private String description;

        @NotNull(message = "Cost price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Cost price cannot be negative")
        private BigDecimal costPrice;

        @NotNull(message = "Selling price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Selling price cannot be negative")
        private BigDecimal sellingPrice;

        private Integer minStockThreshold = 10;
        private String unit = "pcs";

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public String getBarcode() { return barcode; }
        public void setBarcode(String barcode) { this.barcode = barcode; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getCostPrice() { return costPrice; }
        public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
        public BigDecimal getSellingPrice() { return sellingPrice; }
        public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
        public Integer getMinStockThreshold() { return minStockThreshold; }
        public void setMinStockThreshold(Integer minStockThreshold) { this.minStockThreshold = minStockThreshold; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
    }

    public static class ProductResponse {
        private Long id;
        private Long shopId;
        private Long categoryId;
        private String categoryName;
        private String name;
        private String sku;
        private String barcode;
        private String description;
        private BigDecimal costPrice;
        private BigDecimal sellingPrice;
        private Integer currentStock;
        private Integer minStockThreshold;
        private String unit;
        private Boolean isLowStock;
        private Integer activeBatchesCount;
        private LocalDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getShopId() { return shopId; }
        public void setShopId(Long shopId) { this.shopId = shopId; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public String getBarcode() { return barcode; }
        public void setBarcode(String barcode) { this.barcode = barcode; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getCostPrice() { return costPrice; }
        public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
        public BigDecimal getSellingPrice() { return sellingPrice; }
        public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
        public Integer getCurrentStock() { return currentStock; }
        public void setCurrentStock(Integer currentStock) { this.currentStock = currentStock; }
        public Integer getMinStockThreshold() { return minStockThreshold; }
        public void setMinStockThreshold(Integer minStockThreshold) { this.minStockThreshold = minStockThreshold; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public Boolean getIsLowStock() { return isLowStock; }
        public void setIsLowStock(Boolean isLowStock) { this.isLowStock = isLowStock; }
        public Integer getActiveBatchesCount() { return activeBatchesCount; }
        public void setActiveBatchesCount(Integer activeBatchesCount) { this.activeBatchesCount = activeBatchesCount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
