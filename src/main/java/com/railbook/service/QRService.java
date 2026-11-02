package com.railbook.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.railbook.model.QRVerification;
import com.railbook.util.TokenUtil;

import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.util.Base64;

public class QRService {

    public static QRVerification createQRVerification(String bookingId, String pnr) {
        String rawToken = TokenUtil.generateSecureToken(pnr);
        String tokenHash = TokenUtil.hashToken(rawToken);
        String verificationUrl = "https://railbook.internal/ttr/verify/" + rawToken;

        QRVerification qr = new QRVerification();
        qr.setBookingId(bookingId);
        qr.setRawToken(rawToken);
        qr.setSecureTokenHash(tokenHash);
        qr.setVerificationUrl(verificationUrl);
        // Expiration: 72 hours from journey date or creation
        qr.setExpiresAt(new Timestamp(System.currentTimeMillis() + (72L * 3600 * 1000)));
        qr.setStatus("ACTIVE");

        return qr;
    }

    /**
     * Generates Base64 encoded PNG QR Code.
     * CRITICAL SECURITY: Contains ONLY the verification token or URL.
     * Never contains full Aadhaar number, passenger passwords, or confidential IDs.
     */
    public static String generateQRCodeBase64(String tokenOrUrl, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(tokenOrUrl, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] qrBytes = outputStream.toByteArray();

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(qrBytes);
        } catch (Exception e) {
            System.err.println("Error generating QR Code: " + e.getMessage());
            return "";
        }
    }
}
