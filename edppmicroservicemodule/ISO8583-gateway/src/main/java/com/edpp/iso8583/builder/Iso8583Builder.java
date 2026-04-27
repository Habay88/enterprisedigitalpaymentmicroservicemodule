package com.edpp.iso8583.parser;

import com.edpp.iso8583.dto.AuthorizationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * ISO 8583 Builder - Creates ISO 8583 messages from internal DTOs
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Iso8583Builder {

    private final GenericPackager packager;

    /**
     * Build authorization response (0110/0210)
     */
    public ISOMsg buildAuthorizationResponse(ISOMsg request, AuthorizationResponse response) throws Exception {
        ISOMsg reply = (ISOMsg) request.clone();
        reply.setPackager(packager);

        // Set MTI (0110 for authorization response)
        String requestMti = request.getMTI();
        reply.setMTI(getResponseMti(requestMti));

        // Set response code (DE 39)
        reply.set(39, response.responseCode());

        // Set authorization code (DE 38) - 6-digit approval code
        if ("00".equals(response.responseCode())) {
            reply.set(38, generateAuthCode());
        }

        // Set settlement date (DE 60)
        String settlementDate = LocalDateTime.now().plusDays(1)
                .format(DateTimeFormatter.ofPattern("yyMMdd"));
        reply.set(60, settlementDate);

        return reply;
    }

    /**
     * Build reversal response (0410)
     */
    public ISOMsg buildReversalResponse(ISOMsg request, String responseCode, String message) throws Exception {
        ISOMsg reply = (ISOMsg) request.clone();
        reply.setPackager(packager);
        reply.setMTI("0410");
        reply.set(39, responseCode);

        return reply;
    }

    /**
     * Build network management response (0810)
     */
    public ISOMsg buildNetworkResponse(ISOMsg request, String responseCode, String message) throws Exception {
        ISOMsg reply = (ISOMsg) request.clone();
        reply.setPackager(packager);
        reply.setMTI("0810");
        reply.set(39, responseCode);

        return reply;
    }

    private String getResponseMti(String requestMti) {
        int mti = Integer.parseInt(requestMti);
        return String.format("%04d", mti + 100);
    }

    private String generateAuthCode() {
        return String.format("%06d", UUID.randomUUID().hashCode() % 1_000_000);
    }
}