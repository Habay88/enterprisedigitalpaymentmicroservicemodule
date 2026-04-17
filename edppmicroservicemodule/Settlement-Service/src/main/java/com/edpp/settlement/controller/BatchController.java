package com.edpp.settlement.controller;

import com.edpp.settlement.dto.request.CreateBatchRequest;
import com.edpp.settlement.dto.response.BatchSummaryResponse;
import com.edpp.settlement.entity.SettlementBatch;
import com.edpp.settlement.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settlement/batches")
@RequiredArgsConstructor
public class BatchController {

    private final SettlementService settlementService;

    @PostMapping
    @Operation(summary = "Create settlement batch")
    public ResponseEntity<BatchSummaryResponse> createBatch(@Valid @RequestBody CreateBatchRequest request) {
        SettlementBatch batch = settlementService.createBatch(request);
        return new ResponseEntity<>(toResponse(batch), HttpStatus.CREATED);
    }

    @PostMapping("/{batchId}/process")
    @Operation(summary = "Process settlement batch")
    public ResponseEntity<Void> processBatch(@PathVariable String batchId) {
        settlementService.processBatch(batchId);
        return ResponseEntity.accepted().build();
    }

    private BatchSummaryResponse toResponse(SettlementBatch batch) {
        return new BatchSummaryResponse(
            batch.getBatchReference(),
            batch.getBatchDate(),
            batch.getFrequency().name(),
            batch.getStatus().name(),
            batch.getTotalMerchants(),
            batch.getTotalTransactions(),
            batch.getTotalSettlements(),
            batch.getTotalGrossAmount(),
            batch.getTotalNetAmount()
        );
    }
}