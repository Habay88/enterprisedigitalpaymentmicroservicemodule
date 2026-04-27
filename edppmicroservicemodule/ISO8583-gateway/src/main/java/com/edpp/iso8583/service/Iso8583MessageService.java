package com.edpp.iso8583.service;

import com.edpp.iso8583.client.TransactionServiceClient;
import com.edpp.iso8583.dto.AuthorizationRequest;
import com.edpp.iso8583.dto.AuthorizationResponse;
import com.edpp.iso8583.parser.Iso8583Builder;
import com.edpp.iso8583.parser.Iso8583Parser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ISO 8583 Message Service - Core message processing logic
 *
 * Handles:
 * - Authorization requests (card present transactions)
 * - Financial capture requests
 * - Reversal processing
 * - Network management (echo test, key exchange)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Iso8583MessageService {

    private final Iso8583Parser parser;
    private final Iso8583Builder builder;
    private final TransactionServiceClient transactionClient;
    private final AuthorizationService authorizationService;
    private final ReversalService reversalService;
    private final NetworkManagementService networkService;

    /**
     * Process authorization/financial request
     */
    public void processAuthorization(ISOSource source, ISOMsg request) throws Exception {
        log.info("Processing authorization request - STAN: {}", request.getString(11));

        // Parse ISO message to internal DTO
        AuthorizationRequest authRequest = parser.parseAuthorizationRequest(request);

        // Validate PIN if present
        if (authRequest.pinData() != null) {
            boolean pinValid = authorizationService.validatePin(
                    authRequest.pan(),
                    authRequest.pinData()
            );
            if (!pinValid) {
                sendResponse(source, request, "55", "Invalid PIN");
                return;
            }
        }

        // Check for duplicate STAN
        if (authorizationService.isDuplicateStan(authRequest.stan())) {
            sendResponse(source, request, "94", "Duplicate transaction");
            return;
        }

        // Process transaction through payment platform
        AuthorizationResponse response = transactionClient.processAuthorization(authRequest);

        // Send response back to terminal
        ISOMsg responseMsg = builder.buildAuthorizationResponse(request, response);
        source.send(responseMsg);

        log.info("Authorization response sent - STAN: {}, Response Code: {}",
                authRequest.stan(), response.responseCode());
    }

    /**
     * Process reversal request
     */
    public void processReversal(ISOSource source, ISOMsg request) throws Exception {
        log.info("Processing reversal request - STAN: {}", request.getString(11));

        String originalStan = request.getString(90); // Original STAN
        BigDecimal amount = new BigDecimal(request.getString(4));

        reversalService.processReversal(originalStan, amount);

        // Send reversal response
        ISOMsg response = builder.buildReversalResponse(request, "00", "Approved");
        source.send(response);

        log.info("Reversal processed for original STAN: {}", originalStan);
    }

    /**
     * Process network management request (echo test, key exchange)
     */
    public void processNetworkManagement(ISOSource source, ISOMsg request) throws Exception {
        log.info("Processing network management request - STAN: {}", request.getString(11));

        String functionCode = request.getString(63); // Network function code

        ISOMsg response;

        if ("301".equals(functionCode)) { // Echo test
            response = networkService.processEchoTest(request);
        } else if ("302".equals(functionCode)) { // Key exchange
            response = networkService.processKeyExchange(request);
        } else {
            response = builder.buildNetworkResponse(request, "12", "Invalid function");
        }

        source.send(response);
        log.info("Network management response sent for function: {}", functionCode);
    }

    private void sendResponse(ISOSource source, ISOMsg request,
                              String responseCode, String message) throws Exception {
        ISOMsg response = builder.buildAuthorizationResponse(request,
                new AuthorizationResponse(responseCode, message, null, null));
        source.send(response);
    }
}