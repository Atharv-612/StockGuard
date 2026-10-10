package com.stockguard.controller;

import com.stockguard.dto.ReportDto;
import com.stockguard.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ReportDto.DashboardMetricsDto> getDashboardMetrics() {
        return ResponseEntity.ok(reportService.getDashboardMetrics());
    }

    @GetMapping("/monthly-sales")
    public ResponseEntity<List<ReportDto.MonthlySalesDataDto>> getMonthlySalesTrend() {
        return ResponseEntity.ok(reportService.getMonthlySalesTrend());
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<ReportDto.TopProductDto>> getTopSellingProducts() {
        return ResponseEntity.ok(reportService.getTopSellingProducts());
    }

    @GetMapping("/expiry-wastage")
    public ResponseEntity<Map<String, Object>> getExpiryWastageReport() {
        return ResponseEntity.ok(reportService.getExpiryWastageReport());
    }

    @GetMapping("/export-csv")
    public ResponseEntity<byte[]> exportSalesCsv() {
        String csvData = reportService.exportSalesCsv();
        byte[] bytes = csvData.getBytes();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stockguard-sales-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }
}
