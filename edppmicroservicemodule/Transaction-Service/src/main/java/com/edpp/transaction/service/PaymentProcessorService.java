package com.edpp.transaction.service;

import com.edpp.transaction.entity.Transaction;
import com.edpp.transaction.processor.PaymentProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentProcessorService {

    private final List<PaymentProcessor> processors;
    private final Map<String, PaymentProcessor> processorMap = new ConcurrentHashMap<>();

    /**
     * Select appropriate processor based on transaction details
     */
    public PaymentProcessor selectProcessor(Transaction transaction) {
        // Build processor map if empty
        if (processorMap.isEmpty()) {
            processors.forEach(p -> processorMap.put(p.getProcessorName(), p));
        }

        // Rule-based processor selection
        String currency = transaction.getCurrency();
        BigDecimal amount = transaction.getAmount();

        // Routing rules
        if ("USD".equals(currency) && amount.compareTo(new BigDecimal("10000")) < 0) {
            return getProcessor("STRIPE");
        } else if ("NGN".equals(currency)) {
            return getProcessor("PAYSTACK");
        } else if ("GBP".equals(currency) || "EUR".equals(currency)) {
            return getProcessor("STRIPE");
        } else {
            return getProcessor("PAYPAL");
        }
    }

    /**
     * Get processor by name with fallback
     */
    public PaymentProcessor getProcessor(String name) {
        PaymentProcessor processor = processorMap.get(name);
        if (processor == null) {
            log.warn("Processor {} not found, using default", name);
            return processorMap.values().iterator().next();
        }
        return processor;
    }
}