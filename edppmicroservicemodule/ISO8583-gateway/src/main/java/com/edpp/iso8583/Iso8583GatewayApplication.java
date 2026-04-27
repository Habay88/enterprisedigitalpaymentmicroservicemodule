package com.edpp.iso8583;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
@EnableScheduling
public class Iso8583GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(Iso8583GatewayApplication.class, args);
    }

    /**
     * Load ISO 8583 packager configuration
     * Defines field definitions, lengths, and formats
     */
    @Bean
    public GenericPackager iso8583Packager() {
        try {
            return new GenericPackager(new ClassPathResource("iso8583.xml").getFile());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load ISO8583 packager", e);
        }
    }
}