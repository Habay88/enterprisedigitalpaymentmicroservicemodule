package com.edpp.iso8583.config;

import com.edpp.iso8583.server.Iso8583Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

/**
 * TCP Server Configuration - Manages ISO 8583 server connections
 * 
 * The gateway listens on multiple ports for different connection types:
 * - 8583: Main ISO 8583 traffic
 * - 8584: Network management (echo test, key exchange)
 * - 8585: Reversal traffic
 */
@Configuration
@Slf4j
public class TcpServerConfig {

    @Value("${iso8583.server.port:8583}")
    private int mainPort;

    @Value("${iso8583.server.nm-port:8584}")
    private int networkManagementPort;

    @Value("${iso8583.server.reversal-port:8585}")
    private int reversalPort;

    private Iso8583Server mainServer;
    private Iso8583Server nmServer;
    private Iso8583Server reversalServer;

    @Bean
    public Iso8583Server mainIso8583Server() {
        mainServer = new Iso8583Server(mainPort, "MAIN");
        mainServer.start();
        return mainServer;
    }

    @Bean
    public Iso8583Server networkManagementServer() {
        nmServer = new Iso8583Server(networkManagementPort, "NM");
        nmServer.start();
        return nmServer;
    }

    @Bean
    public Iso8583Server reversalServer() {
        reversalServer = new Iso8583Server(reversalPort, "REVERSAL");
        reversalServer.start();
        return reversalServer;
    }

    @PreDestroy
    public void stopServers() {
        if (mainServer != null)
            mainServer.stop();
        if (nmServer != null)
            nmServer.stop();
        if (reversalServer != null)
            reversalServer.stop();
        log.info("All ISO 8583 servers stopped");
    }
}