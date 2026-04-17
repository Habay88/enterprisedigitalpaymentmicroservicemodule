package com.edpp.settlement.mapper;

import com.edpp.settlement.dto.response.SettlementResponse;
import com.edpp.settlement.entity.Settlement;
import org.springframework.stereotype.Component;

@Component
public class SettlementMapper {

    public SettlementResponse toResponse(Settlement settlement) {
        if (settlement == null) return null;

        return new SettlementResponse(
            settlement.getId(),
            settlement.getSettlementReference(),
            settlement.getBatchId(),
            settlement.getMerchantId(),
            settlement.getMerchantName(),
            settlement.getSettlementDate(),
            settlement.getStatus().name(),
            settlement.getGrossAmount(),
            settlement.getTotalFees(),
            settlement.getNetAmount(),
            settlement.getTransactionCount(),
            settlement.getTransferReference(),
            settlement.getStatus().name(),
            settlement.getCreatedAt()
        );
    }
}