package com.railbook.model;

import java.sql.Timestamp;

public class QRVerification {
    private int qrId;
    private String bookingId;
    private String secureTokenHash;
    private String rawToken;
    private String verificationUrl;
    private Timestamp expiresAt;
    private String status; // ACTIVE, REVOKED, EXPIRED
    private Timestamp createdAt;

    public QRVerification() {}

    public int getQrId() { return qrId; }
    public void setQrId(int qrId) { this.qrId = qrId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getSecureTokenHash() { return secureTokenHash; }
    public void setSecureTokenHash(String secureTokenHash) { this.secureTokenHash = secureTokenHash; }

    public String getRawToken() { return rawToken; }
    public void setRawToken(String rawToken) { this.rawToken = rawToken; }

    public String getVerificationUrl() { return verificationUrl; }
    public void setVerificationUrl(String verificationUrl) { this.verificationUrl = verificationUrl; }

    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
