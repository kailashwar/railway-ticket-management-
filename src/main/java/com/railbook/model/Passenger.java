package com.railbook.model;

import java.sql.Timestamp;

public class Passenger {
    private int passengerId;
    private String bookingId;
    private String name;
    private int age;
    private String gender;
    private String berthPreference;
    private String nationality;
    private String mobileNumber;
    private String coach;
    private String seat;
    private String status; // CONFIRMED, WAITING LIST, NOT SELECTED, CANCELLED
    private boolean seniorCitizen;
    private Timestamp createdAt;

    // Associated Identity Document
    private IdentityDocument identityDocument;

    public Passenger() {}

    // Getters and Setters
    public int getPassengerId() { return passengerId; }
    public void setPassengerId(int passengerId) { this.passengerId = passengerId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) {
        this.age = age;
        if (age > 60) {
            this.seniorCitizen = true;
        }
    }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBerthPreference() { return berthPreference; }
    public void setBerthPreference(String berthPreference) { this.berthPreference = berthPreference; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getCoach() { return coach; }
    public void setCoach(String coach) { this.coach = coach; }

    public String getSeat() { return seat; }
    public void setSeat(String seat) { this.seat = seat; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isSeniorCitizen() { return seniorCitizen; }
    public void setSeniorCitizen(boolean seniorCitizen) { this.seniorCitizen = seniorCitizen; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public IdentityDocument getIdentityDocument() { return identityDocument; }
    public void setIdentityDocument(IdentityDocument identityDocument) { this.identityDocument = identityDocument; }
}
