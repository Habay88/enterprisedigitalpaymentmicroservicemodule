package com.edpp.iso8583.parser;

import com.edpp.iso8583.dto.AuthorizationRequest;
import com.edpp.iso8583.enums.ProcessingCode;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ISO 8583 Parser - Converts ISO 8583 messages to internal DTOs
 *
 * Extracts fields from ISO message based on ISO 8583 specification
 */
@Component
public class Iso8583Parser {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMdd");

    /**
     * Parse authorization request (MTI 0100/0200)
     */
    public AuthorizationRequest parseAuthorizationRequest(ISOMsg isoMsg) {
        String pan = isoMsg.getString(2);           // Primary Account Number
        String processingCode = isoMsg.getString(3); // Processing Code
        String amount = isoMsg.getString(4);         // Transaction amount
        String stan = isoMsg.getString(11);          // STAN
        String time = isoMsg.getString(12);          // Local transaction time
        String date = isoMsg.getString(13);          // Local transaction date
        String posEntryMode = isoMsg.getString(22);  // POS entry mode
        String terminalId = isoMsg.getString(41);    // Terminal ID
        String merchantId = isoMsg.getString(42);    // Merchant ID
        String pinData = isoMsg.getString(52);       // PIN data (if present)
        String emvData = isoMsg.getString(55);       // EMV chip data (if present)

        return new AuthorizationRequest(
                pan,
                ProcessingCode.fromCode(processingCode),
                new BigDecimal(amount).divide(new BigDecimal("100")), // Convert from cents
                stan,
                parseTransactionTime(time, date),
                posEntryMode,
                terminalId,
                merchantId,
                pinData,
                emvData
        );
    }

    private LocalDateTime parseTransactionTime(String time, String date) {
        // Time format: HHmmss, Date format: MMDD
        String dateTimeStr = LocalDateTime.now().getYear() + date + time;
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
}