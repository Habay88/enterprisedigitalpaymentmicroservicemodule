package com.edpp.iso8583.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * MAC (Message Authentication Code) Calculator
 * Used for message integrity verification in ISO 8583
 */
@Component
@Slf4j
public class MacCalculator {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Calculate MAC using DES/3DES algorithm
     */
    public String calculateMac(String message, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(hexStringToByteArray(key), "DESede");
            Mac mac = Mac.getInstance("DESede/CBC/NoPadding", "BC");
            mac.init(keySpec);
            byte[] macBytes = mac.doFinal(message.getBytes());
            return bytesToHex(macBytes);
        } catch (Exception e) {
            log.error("MAC calculation failed", e);
            return null;
        }
    }

    /**
     * Calculate SHA-256 hash for additional security
     */
    public String calculateSha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes("UTF-8"));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("SHA-256 calculation failed", e);
            return null;
        }
    }

    /**
     * Verify MAC
     */
    public boolean verifyMac(String message, String key, String expectedMac) {
        String calculatedMac = calculateMac(message, key);
        return calculatedMac != null && calculatedMac.equals(expectedMac);
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}