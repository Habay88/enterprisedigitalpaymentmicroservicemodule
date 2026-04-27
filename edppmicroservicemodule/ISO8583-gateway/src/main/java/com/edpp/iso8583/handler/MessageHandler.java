package com.edpp.iso8583.handler;

import com.edpp.iso8583.service.Iso8583MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.springframework.stereotype.Component;

/**
 * Message Handler - Processes incoming ISO 8583 messages
 *
 * Handles different message types:
 * - 0100: Authorization Request
 * - 0200: Financial Request (Capture)
 * - 0400: Reversal Request
 * - 0800: Network Management Request
 * - 0420: Reversal Advice
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageHandler implements ISORequestListener {

    private final Iso8583MessageService messageService;

    @Override
    public boolean process(ISOSource source, ISOMsg isoMsg) {
        try {
            String mti = isoMsg.getMTI();
            log.info("Received ISO 8583 message - MTI: {}, STAN: {}",
                    mti, isoMsg.getString(11));

            // Route based on MTI
            switch (mti) {
                case "0100": // Authorization Request
                case "0200": // Financial Request
                    messageService.processAuthorization(source, isoMsg);
                    break;

                case "0400": // Reversal Request
                    messageService.processReversal(source, isoMsg);
                    break;

                case "0800": // Network Management Request
                    messageService.processNetworkManagement(source, isoMsg);
                    break;

                default:
                    log.warn("Unsupported MTI: {}", mti);
                    sendErrorResponse(source, isoMsg, "12", "Invalid transaction");
            }

        } catch (Exception e) {
            log.error("Error processing ISO 8583 message", e);
            sendErrorResponse(source, isoMsg, "96", "System malfunction");
        }

        return true;
    }

    private void sendErrorResponse(ISOSource source, ISOMsg request,
                                   String responseCode, String message) {
        try {
            ISOMsg response = (ISOMsg) request.clone();
            response.setMTI(getResponseMti(request.getMTI()));
            response.set(39, responseCode);
            source.send(response);
            log.info("Sent error response: {} - {}", responseCode, message);
        } catch (Exception e) {
            log.error("Failed to send error response", e);
        }
    }

    private String getResponseMti(String requestMti) {
        // 0100 -> 0110, 0200 -> 0210, 0400 -> 0410, 0800 -> 0810
        int mti = Integer.parseInt(requestMti);
        return String.format("%04d", mti + 100);
    }
}