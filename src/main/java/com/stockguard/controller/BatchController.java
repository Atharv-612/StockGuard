package com.stockguard.controller;

import com.stockguard.dto.BatchDto;
import com.stockguard.service.BatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @GetMapping
    public ResponseEntity<List<BatchDto.BatchResponse>> getAllBatches(
            @RequestParam(required = false) String filter
    ) {
        return ResponseEntity.ok(batchService.getAllBatches(filter));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<BatchDto.BatchResponse>> getBatchesByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(batchService.getBatchesByProduct(productId));
    }

    @PostMapping
    public ResponseEntity<BatchDto.BatchResponse> createBatch(@Valid @RequestBody BatchDto.BatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(batchService.createBatch(request));
    }

    @PostMapping("/{id}/discard")
    public ResponseEntity<Map<String, String>> discardExpiredBatch(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        String reason = payload != null ? payload.get("reason") : "Discarded expired stock";
        batchService.discardExpiredBatch(id, reason);
        return ResponseEntity.ok(Map.of("message", "Batch successfully marked as discarded"));
    }
}
