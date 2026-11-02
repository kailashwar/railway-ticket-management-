package com.railbook.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

public class Booking {
    private String bookingId;
    private String pnr;
    private int userId;
    private int trainId;
    private Date journeyDate;
    private String source;
    private String destination;
    private String travelClass;
    private String quota;
    private BigDecimal fare;
    private String status; // CONFIRMED, WAITING LIST, NOT SELECTED, CANCELLED
    private Timestamp createdAt;

    // Joined fields for display
    private String trainNumber;
    private String trainName;
    private String userName;
    private String userPhone;
    private String userEmail;
    private List<Passenger> passengers;
    private QRVerification qrVerification;

    public Booking() {}

    // Getters and Setters
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getTrainId() { return trainId; }
    public void setTrainId(int trainId) { this.trainId = trainId; }

    public Date getJourneyDate() { return journeyDate; }
    public void setJourneyDate(Date journeyDate) { this.journeyDate = journeyDate; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getTravelClass() { return travelClass; }
    public void setTravelClass(String travelClass) { this.travelClass = travelClass; }

    public String getQuota() { return quota; }
    public void setQuota(String quota) { this.quota = quota; }

    public BigDecimal getFare() { return fare; }
    public void setFare(BigDecimal fare) { this.fare = fare; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getTrainNumber() { return trainNumber; }
    public void setTrainNumber(String trainNumber) { this.trainNumber = trainNumber; }

    public String getTrainName() { return trainName; }
    public void setTrainName(String trainName) { this.trainName = trainName; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public List<Passenger> getPassengers() { return passengers; }
    public void setPassengers(List<Passenger> passengers) { this.passengers = passengers; }

    public QRVerification getQrVerification() { return qrVerification; }
    public void setQrVerification(QRVerification qrVerification) { this.qrVerification = qrVerification; }
}
