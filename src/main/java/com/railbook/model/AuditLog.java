package com.railbook.model;

import java.sql.Timestamp;

public class AuditLog {
    private int logId;
    private String ttrId;
    private String bookingId;
    private Timestamp verificationTime;
    private String result; // VERIFIED, FAILED, INVALID_TOKEN, CANCELLED
    private String action; // QR_SCAN, MANUAL_INPUT, VIEW_IDENTITY_DOC
    private String ipAddress;
    private String userAgent;
    private String notes;

    // Display fields
    private String ttrName;
    private String pnr;

    public AuditLog() {}

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public String getTtrId() { return ttrId; }
    public void setTtrId(String ttrId) { this.ttrId = ttrId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public Timestamp getVerificationTime() { return verificationTime; }
    public void setVerificationTime(Timestamp verificationTime) { this.verificationTime = verificationTime; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getTtrName() { return ttrName; }
    public void setTtrName(String ttrName) { this.ttrName = ttrName; }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }
}
