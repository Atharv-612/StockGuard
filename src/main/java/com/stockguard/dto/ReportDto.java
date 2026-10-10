package com.stockguard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ReportDto {

    public static class DashboardMetricsDto {
        private long totalProducts;
        private long totalStockQuantity;
        private long lowStockCount;
        private long expiringSoonCount;
        private long expiredCount;
        private BigDecimal todaySales;
        private BigDecimal monthlySales;
        private long todayOrdersCount;
        private String currency;

        public long getTotalProducts() { return totalProducts; }
        public void setTotalProducts(long totalProducts) { this.totalProducts = totalProducts; }
        public long getTotalStockQuantity() { return totalStockQuantity; }
        public void setTotalStockQuantity(long totalStockQuantity) { this.totalStockQuantity = totalStockQuantity; }
        public long getLowStockCount() { return lowStockCount; }
        public void setLowStockCount(long lowStockCount) { this.lowStockCount = lowStockCount; }
        public long getExpiringSoonCount() { return expiringSoonCount; }
        public void setExpiringSoonCount(long expiringSoonCount) { this.expiringSoonCount = expiringSoonCount; }
        public long getExpiredCount() { return expiredCount; }
        public void setExpiredCount(long expiredCount) { this.expiredCount = expiredCount; }
        public BigDecimal getTodaySales() { return todaySales; }
        public void setTodaySales(BigDecimal todaySales) { this.todaySales = todaySales; }
        public BigDecimal getMonthlySales() { return monthlySales; }
        public void setMonthlySales(BigDecimal monthlySales) { this.monthlySales = monthlySales; }
        public long getTodayOrdersCount() { return todayOrdersCount; }
        public void setTodayOrdersCount(long todayOrdersCount) { this.todayOrdersCount = todayOrdersCount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

    public static class MonthlySalesDataDto {
        private String label;
        private BigDecimal revenue;
        private long orderCount;

        public MonthlySalesDataDto() {}
        public MonthlySalesDataDto(String label, BigDecimal revenue, long orderCount) {
            this.label = label;
            this.revenue = revenue;
            this.orderCount = orderCount;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
        public long getOrderCount() { return orderCount; }
        public void setOrderCount(long orderCount) { this.orderCount = orderCount; }
    }

    public static class TopProductDto {
        private Long productId;
        private String productName;
        private long unitsSold;
        private BigDecimal totalRevenue;

        public TopProductDto() {}
        public TopProductDto(Long productId, String productName, long unitsSold, BigDecimal totalRevenue) {
            this.productId = productId;
            this.productName = productName;
            this.unitsSold = unitsSold;
            this.totalRevenue = totalRevenue;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public long getUnitsSold() { return unitsSold; }
        public void setUnitsSold(long unitsSold) { this.unitsSold = unitsSold; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    }

    public static class StockMovementDto {
        private Long id;
        private String productName;
        private String batchNumber;
        private String movementType;
        private Integer quantityChanged;
        private Integer quantityAfter;
        private String reference;
        private String notes;
        private LocalDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
        public String getMovementType() { return movementType; }
        public void setMovementType(String movementType) { this.movementType = movementType; }
        public Integer getQuantityChanged() { return quantityChanged; }
        public void setQuantityChanged(Integer quantityChanged) { this.quantityChanged = quantityChanged; }
        public Integer getQuantityAfter() { return quantityAfter; }
        public void setQuantityAfter(Integer quantityAfter) { this.quantityAfter = quantityAfter; }
        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
