package com.edpp.transaction.processor;

import com.edpp.transaction.dtoresponse.ProcessorResponse;
import com.edpp.transaction.entity.Transaction;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@Slf4j
public class StripeProcessor implements PaymentProcessor {

    @Value("${stripe.api-key}")
    private String apiKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }

    @Override
    public ProcessorResponse processPayment(Transaction transaction) {
        log.info("Processing payment via Stripe: {}", transaction.getTransactionReference());

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(transaction.getAmount()
                            .multiply(new BigDecimal("100"))
                            .longValue()) // Convert to cents
                    .setCurrency(transaction.getCurrency().toLowerCase())
                    .setDescription(transaction.getDescription())
                    .putMetadata("transactionReference", transaction.getTransactionReference())
                    .putMetadata("tenantId", transaction.getTenantId())
                    .putMetadata("customerEmail", transaction.getCustomerEmail())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            return ProcessorResponse.builder()
                    .successful(true)
                    .transactionId(intent.getId())
                    .responseCode(intent.getStatus())
                    .responseMessage("Payment intent created")
                    .rawResponse(intent.toJson())
                    .processedAt(LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(intent.getCreated()), 
                            ZoneId.systemDefault()))
                    .build();

        } catch (StripeException e) {
            log.error("Stripe payment failed", e);
            return ProcessorResponse.builder()
                    .successful(false)
                    .responseCode(e.getCode())
                    .responseMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public ProcessorResponse processRefund(String transactionId, BigDecimal amount) {
        try {
            // Implement refund logic
            return ProcessorResponse.builder()
                    .successful(true)
                    .transactionId(transactionId + "-refund")
                    .build();
        } catch (Exception e) {
            return ProcessorResponse.builder()
                    .successful(false)
                    .responseMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public ProcessorResponse getTransactionStatus(String processorTransactionId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(processorTransactionId);
            
            return ProcessorResponse.builder()
                    .successful(true)
                    .transactionId(intent.getId())
                    .responseCode(intent.getStatus())
                    .responseMessage("Status retrieved")
                    .build();
                    
        } catch (StripeException e) {
            return ProcessorResponse.builder()
                    .successful(false)
                    .responseMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public ProcessorResponse verifyWebhookSignature(String payload, String signature) {
        // Implement webhook signature verification
        return ProcessorResponse.builder()
                .successful(true)
                .build();
    }

    @Override
    public String getProcessorName() {
        return "STRIPE";
    }
}
