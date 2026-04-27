package com.edpp.iso8583.server;

import com.edpp.iso8583.handler.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOServer;
import org.jpos.iso.ServerChannel;
import org.jpos.iso.channel.NACChannel;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * ISO 8583 Server - Listens for incoming ISO 8583 messages
 *
 * This server handles connections from:
 * - POS Terminals (Verifone, Ingenico, PAX)
 * - ATMs (NCR, Diebold, Wincor)
 * - Bank switches (NIBSS, Interswitch)
 *
 * Features:
 * - Persistent TCP connections
 * - Message parsing and validation
 * - Request/response correlation
 * - Connection pooling
 */
@Component
@Slf4j
public class Iso8583Server {

    private final int port;
    private final String serverType;
    private ISOServer server;
    private GenericPackager packager;

    @Autowired
    private MessageHandler messageHandler;

    public Iso8583Server(int port, String serverType) {
        this.port = port;
        this.serverType = serverType;
    }

    /**
     * Start the ISO 8583 server
     */
    public void start() {
        try {
            // Load packager
            packager = new GenericPackager("iso8583.xml");

            // Create server channel
            ServerChannel channel = new NACChannel(packager);

            // Create ISO Server
            server = new ISOServer(port, channel, null);

            // Set message handler
            server.addISORequestListener(messageHandler);

            // Start server
            server.start();

            log.info("ISO 8583 Server started on port {} - Type: {}", port, serverType);

        } catch (Exception e) {
            log.error("Failed to start ISO 8583 server on port {}", port, e);
            throw new RuntimeException("ISO 8583 server startup failed", e);
        }
    }

    /**
     * Stop the server
     */
    public void stop() {
        if (server != null) {
            server.shutdown();
            log.info("ISO 8583 Server stopped on port {}", port);
        }
    }

    public int getPort() {
        return port;
    }
}