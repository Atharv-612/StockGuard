package com.stockguard.service;

import com.stockguard.dto.ReportDto;
import com.stockguard.entity.ProductBatch;
import com.stockguard.entity.Sale;
import com.stockguard.entity.Shop;
import com.stockguard.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportService {

    private final ProductRepository productRepository;
    private final ProductBatchRepository batchRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final AuthService authService;

    public ReportService(ProductRepository productRepository,
                         ProductBatchRepository batchRepository,
                         SaleRepository saleRepository,
                         SaleItemRepository saleItemRepository,
                         AuthService authService) {
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.authService = authService;
    }

    public ReportDto.DashboardMetricsDto getDashboardMetrics() {
        Shop shop = authService.getCurrentUserShop();
        LocalDate today = LocalDate.now();
        int warningDays = shop.getExpiryWarningDays() != null ? shop.getExpiryWarningDays() : 7;
        LocalDate warningDate = today.plusDays(warningDays);

        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();

        ReportDto.DashboardMetricsDto dto = new ReportDto.DashboardMetricsDto();
        dto.setTotalProducts(productRepository.countByShopId(shop.getId()));
        dto.setTotalStockQuantity(productRepository.sumTotalStockQuantityByShopId(shop.getId()));
        dto.setLowStockCount(productRepository.findLowStockProducts(shop.getId()).size());
        dto.setExpiringSoonCount(batchRepository.countExpiringSoonBatches(shop.getId(), today, warningDate));
        dto.setExpiredCount(batchRepository.countExpiredBatches(shop.getId(), today));

        dto.setTodaySales(saleRepository.sumTotalSalesBetween(shop.getId(), startOfToday, endOfToday));
        dto.setMonthlySales(saleRepository.sumTotalSalesBetween(shop.getId(), startOfMonth, endOfToday));
        dto.setTodayOrdersCount(saleRepository.countSalesBetween(shop.getId(), startOfToday, endOfToday));
        dto.setCurrency(shop.getCurrency());

        return dto;
    }

    public List<ReportDto.MonthlySalesDataDto> getMonthlySalesTrend() {
        Shop shop = authService.getCurrentUserShop();
        LocalDate today = LocalDate.now();

        List<ReportDto.MonthlySalesDataDto> trends = new ArrayList<>();
        // Past 6 months
        for (int i = 5; i >= 0; i--) {
            LocalDate monthTarget = today.minusMonths(i);
            LocalDateTime start = monthTarget.withDayOfMonth(1).atStartOfDay();
            LocalDateTime end = monthTarget.withDayOfMonth(monthTarget.lengthOfMonth()).atTime(LocalTime.MAX);

            BigDecimal revenue = saleRepository.sumTotalSalesBetween(shop.getId(), start, end);
            long orders = saleRepository.countSalesBetween(shop.getId(), start, end);

            String label = monthTarget.format(DateTimeFormatter.ofPattern("MMM yyyy"));
            trends.add(new ReportDto.MonthlySalesDataDto(label, revenue != null ? revenue : BigDecimal.ZERO, orders));
        }

        return trends;
    }

    public List<ReportDto.TopProductDto> getTopSellingProducts() {
        Shop shop = authService.getCurrentUserShop();
        LocalDateTime startOfMonth = LocalDate.now().minusMonths(3).atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        List<Object[]> rawList = saleItemRepository.findTopSellingProducts(shop.getId(), startOfMonth, end);
        List<ReportDto.TopProductDto> topList = new ArrayList<>();

        for (Object[] row : rawList) {
            Long prodId = (Long) row[0];
            String prodName = (String) row[1];
            long qty = ((Number) row[2]).longValue();
            BigDecimal rev = (BigDecimal) row[3];
            topList.add(new ReportDto.TopProductDto(prodId, prodName, qty, rev));
        }

        return topList;
    }

    public Map<String, Object> getExpiryWastageReport() {
        Shop shop = authService.getCurrentUserShop();
        LocalDate today = LocalDate.now();

        List<ProductBatch> expiredBatches = batchRepository.findExpiredBatches(shop.getId(), today);
        long totalUnits = 0;
        BigDecimal totalLoss = BigDecimal.ZERO;

        List<Map<String, Object>> items = new ArrayList<>();
        for (ProductBatch b : expiredBatches) {
            totalUnits += b.getCurrentQuantity();
            BigDecimal itemLoss = b.getCostPrice().multiply(BigDecimal.valueOf(b.getCurrentQuantity()));
            totalLoss = totalLoss.add(itemLoss);

            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("productName", b.getProduct().getName());
            itemMap.put("batchNumber", b.getBatchNumber());
            itemMap.put("expiredQuantity", b.getCurrentQuantity());
            itemMap.put("costPrice", b.getCostPrice());
            itemMap.put("lossAmount", itemLoss);
            itemMap.put("expiryDate", b.getExpiryDate());
            items.add(itemMap);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("totalExpiredBatches", expiredBatches.size());
        report.put("totalExpiredUnits", totalUnits);
        report.put("estimatedLossAmount", totalLoss);
        report.put("currency", shop.getCurrency());
        report.put("items", items);
        return report;
    }

    public String exportSalesCsv() {
        Shop shop = authService.getCurrentUserShop();
        List<Sale> sales = saleRepository.findByShopIdOrderBySaleDateDesc(shop.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("Invoice Number,Date,Customer,Payment Method,Subtotal,Discount,Total Amount\n");

        for (Sale s : sales) {
            sb.append(escapeCsv(s.getInvoiceNumber())).append(",")
              .append(escapeCsv(s.getSaleDate().toString())).append(",")
              .append(escapeCsv(s.getCustomerName())).append(",")
              .append(escapeCsv(s.getPaymentMethod())).append(",")
              .append(s.getSubtotal()).append(",")
              .append(s.getDiscount()).append(",")
              .append(s.getTotalAmount()).append("\n");
        }

        return sb.toString();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
