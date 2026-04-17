package com.edpp.settlement.service;

import com.edpp.settlement.client.BankAPIClient;
import com.edpp.settlement.dto.response.BankTransferResponse;
import com.edpp.settlement.entity.BankTransferRecord;
import com.edpp.settlement.entity.Settlement;
import com.edpp.settlement.enums.SettlementStatus;
import com.edpp.settlement.enums.TransferStatus;
import com.edpp.settlement.repository.BankTransferRecordRepository;
import com.edpp.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankTransferService {

    private final BankTransferRecordRepository transferRepository;
    private final SettlementRepository settlementRepository;
    private final BankAPIClient bankAPIClient;
    private final WebhookNotificationService webhookService;

    /**
     * Initiate bank transfer for a settlement
     */
    @Transactional
    public BankTransferResponse initiateTransfer(Settlement settlement) {
        log.info("Initiating bank transfer for settlement: {}", settlement.getSettlementReference());

        String transferReference = generateTransferReference();
        
        BankTransferRecord transfer = BankTransferRecord.builder()
                .transferReference(transferReference)
                .settlementId(settlement.getId())
                .merchantId(settlement.getMerchantId())
                .bankAccountNumber(settlement.getBankAccountNumber())
                .bankCode(settlement.getBankCode())
                .bankName(settlement.getBankName())
                .amount(settlement.getNetAmount())
                .currency("NGN")
                .status(TransferStatus.PENDING)
                .initiatedAt(LocalDateTime.now())
                .retryCount(0)
                .tenantId(settlement.getTenantId())
                .build();

        transferRepository.save(transfer);

        // Update settlement status
        settlement.setStatus(SettlementStatus.PROCESSING);
        settlement.setTransferReference(transferReference);
        settlement.setTransferInitiatedAt(LocalDateTime.now());
        settlementRepository.save(settlement);

        // Call bank API asynchronously
        processTransferAsync(transfer);

        return new BankTransferResponse(
            transferReference,
            settlement.getId(),
            settlement.getNetAmount(),
            "PENDING",
            null,
            null,
            LocalDateTime.now()
        );
    }

    /**
     * Process transfer asynchronously
     */
    @Async
    @Transactional
    public void processTransferAsync(BankTransferRecord transfer) {
        try {
            transfer.setStatus(TransferStatus.PROCESSING);
            transferRepository.save(transfer);

            // Call bank API
            var response = bankAPIClient.transferFunds(
                transfer.getBankAccountNumber(),
                transfer.getBankCode(),
                transfer.getAmount(),
                transfer.getTransferReference()
            );

            if (response.isSuccess()) {
                transfer.setStatus(TransferStatus.SUCCESS);
                transfer.setResponseCode(response.responseCode());
                transfer.setResponseMessage(response.message());
                transfer.setCompletedAt(LocalDateTime.now());
                
                // Update settlement
                Settlement settlement = settlementRepository.findById(transfer.getSettlementId()).orElseThrow();
                settlement.setStatus(SettlementStatus.COMPLETED);
                settlement.setTransferCompletedAt(LocalDateTime.now());
                settlementRepository.save(settlement);
                
                // Send webhook notification
                webhookService.notifyMerchant(settlement);
                
                log.info("Transfer completed successfully: {}", transfer.getTransferReference());
            } else {
                handleTransferFailure(transfer, response.message());
            }
        } catch (Exception e) {
            log.error("Transfer failed: {}", transfer.getTransferReference(), e);
            handleTransferFailure(transfer, e.getMessage());
        }
    }

    private void handleTransferFailure(BankTransferRecord transfer, String errorMessage) {
        transfer.setStatus(TransferStatus.FAILED);
        transfer.setResponseMessage(errorMessage);
        
        if (transfer.getRetryCount() < 3) {
            transfer.setRetryCount(transfer.getRetryCount() + 1);
            transfer.setStatus(TransferStatus.PENDING);
            log.info("Retrying transfer (attempt {}): {}", transfer.getRetryCount(), transfer.getTransferReference());
            transferRepository.save(transfer);
            processTransferAsync(transfer);
        } else {
            transfer.setStatus(TransferStatus.FAILED);
            transfer.setResponseMessage("Max retries exceeded: " + errorMessage);
            transferRepository.save(transfer);
            
            Settlement settlement = settlementRepository.findById(transfer.getSettlementId()).orElseThrow();
            settlement.setStatus(SettlementStatus.FAILED);
            settlement.setRejectionReason(errorMessage);
            settlementRepository.save(settlement);
        }
    }

    private String generateTransferReference() {
        return "TRF_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}