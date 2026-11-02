package com.railbook.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class TokenUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generatePNR() {
        // 10-digit railway standard PNR
        long firstPart = 1000 + RANDOM.nextInt(9000);
        long secondPart = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(firstPart) + String.valueOf(secondPart);
    }

    public static String generateBookingId() {
        long timestamp = System.currentTimeMillis() % 1000000;
        int randomCode = 100 + RANDOM.nextInt(900);
        return "RB" + timestamp + randomCode;
    }

    public static String generateSecureToken(String pnr) {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return "RB-SECURE-" + pnr + "-" + sb.toString();
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public static String maskIdentifier(String rawNumber) {
        if (rawNumber == null || rawNumber.length() < 4) {
            return "XXXX XXXX 1234";
        }
        String clean = rawNumber.replaceAll("\\s+", "");
        String lastFour = clean.substring(Math.max(0, clean.length() - 4));
        return "XXXX XXXX " + lastFour;
    }
}
