package com.railbook.model;

import java.sql.Timestamp;

public class IdentityDocument {
    private int documentId;
    private int passengerId;
    private String documentType; // Aadhaar Card (Demo)
    private String fileName;
    private String encryptedFileReference;
    private String maskedIdentifier; // e.g. XXXX XXXX 1234
    private String verificationStatus; // VERIFIED, PENDING, REJECTED
    private Timestamp uploadedAt;

    public IdentityDocument() {}

    public int getDocumentId() { return documentId; }
    public void setDocumentId(int documentId) { this.documentId = documentId; }

    public int getPassengerId() { return passengerId; }
    public void setPassengerId(int passengerId) { this.passengerId = passengerId; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getEncryptedFileReference() { return encryptedFileReference; }
    public void setEncryptedFileReference(String encryptedFileReference) { this.encryptedFileReference = encryptedFileReference; }

    public String getMaskedIdentifier() { return maskedIdentifier; }
    public void setMaskedIdentifier(String maskedIdentifier) { this.maskedIdentifier = maskedIdentifier; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public Timestamp getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Timestamp uploadedAt) { this.uploadedAt = uploadedAt; }
}
