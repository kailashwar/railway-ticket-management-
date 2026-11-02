package com.railbook.model;

import java.sql.Timestamp;

public class TTRUser {
    private String ttrId;
    private String name;
    private String username;
    private String badgeNumber;
    private String division;
    private String passwordHash;
    private String status;
    private Timestamp createdAt;

    public TTRUser() {}

    public String getTtrId() { return ttrId; }
    public void setTtrId(String ttrId) { this.ttrId = ttrId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getBadgeNumber() { return badgeNumber; }
    public void setBadgeNumber(String badgeNumber) { this.badgeNumber = badgeNumber; }

    public String getDivision() { return division; }
    public void setDivision(String division) { this.division = division; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
