package com.edpp.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main entry point for the Ledger Service
 * 
 * Ledger Service is responsible for:
 * - Maintaining financial ledger records
 * - Recording all financial transactions
 * - Providing ledger query and reporting capabilities
 * - Ensuring financial data integrity and audit trails
 */
@SpringBootApplication
@EnableDiscoveryClient
public class LedgerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerServiceApplication.class, args);
    }
}
