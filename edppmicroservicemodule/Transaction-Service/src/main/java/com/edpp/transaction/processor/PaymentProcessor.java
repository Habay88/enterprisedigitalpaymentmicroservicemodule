package com.edpp.transaction.processor;

import com.edpp.transaction.dtoresponse.ProcessorResponse;
import com.edpp.transaction.entity.Transaction;
import java.math.BigDecimal;



public interface PaymentProcessor {
    
    ProcessorResponse processPayment(Transaction transaction);
    
    default ProcessorResponse processRefund(String transactionId, BigDecimal amount) {
        return ProcessorResponse.failure("Refund not supported by this processor");
    }
    
    default ProcessorResponse getTransactionStatus(String processorTransactionId) {
        return ProcessorResponse.failure("Status check not supported by this processor");
    }
    
    default ProcessorResponse verifyWebhookSignature(String payload, String signature) {
        return ProcessorResponse.success("Webhook verified");
    }
    
    String getProcessorName();
}

   /*  ProcessorResponse processPayment(Transaction transaction);
    ProcessorResponse processRefund(String transactionId, BigDecimal amount);
   P rocessorResponse getTransactionStatus(String processorTransactionId);
    ProcessorResponse verifyWebhookSignature(String payload, String signature);
    String getProcessorName(); */

