/* package com.edpp.transaction.processor;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.edpp.transaction.dtoresponse.ProcessorResponse;
import com.edpp.transaction.entity.Transaction;

import java.math.BigDecimal;

@Component
@Slf4j
public class PayPalProcessor implements PaymentProcessor {

    @Value("${paypal.client-id:}")
    private String clientId;

    @Value("${paypal.client-secret:}")
    private String clientSecret;

    @Override
    public ProcessorResponse processPayment(Transaction transaction) {
        log.info("Processing payment via PayPal: {}", transaction.getTransactionReference());

        // Check if PayPal is configured
        if (clientId == null || clientId.isEmpty()) {
            log.warn("PayPal not configured, simulating successful payment");
            return simulatePayPalPayment(transaction);
        }

        // Implement actual PayPal integration here
        // For now, return simulated response
        return ProcessorResponse.builder()
                .successful(true)
                .transactionId("pp_sim_" + transaction.getTransactionReference())
                .responseCode("00")
                .responseMessage("PayPal payment simulated")
                .processedAt(java.time.LocalDateTime.now())
                .authorizationUrl("https://www.paypal.com/checkoutnow?token=sim_" + 
                                 transaction.getTransactionReference())
                .build();
    }

      private ProcessorResponse simulatePayPalPayment(Transaction transaction) {
        return ProcessorResponse.builder()
                .successful(true)
                .transactionId("pp_sim_" + transaction.getTransactionReference())
                .responseCode("00")
                .responseMessage("Simulated PayPal payment")
                .processedAt(java.time.LocalDateTime.now())
                .build();
    }

    @Override
    public ProcessorResponse processRefund(String transactionId, BigDecimal amount) {
        log.info("Processing refund via PayPal: {}", transactionId);
        return ProcessorResponse.success(transactionId + "-refund");
    }

    @Override
    public String getProcessorName() {
        return "PAYPAL";
    }
}


 */