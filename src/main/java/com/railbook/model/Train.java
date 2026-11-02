package com.railbook.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Train {
    private int trainId;
    private String trainNumber;
    private String trainName;
    private String source;
    private String destination;
    private String departureTime;
    private String arrivalTime;
    private String duration;
    private int totalSeats;
    private int availableSeats;
    private int acSeats;
    private int sleeperSeats;
    private int tatkalSeats;
    private BigDecimal fareAc;
    private BigDecimal fareSleeper;
    private BigDecimal fareGeneral;
    private BigDecimal tatkalSurcharge;
    private String runsOn;
    private Timestamp createdAt;

    public Train() {}

    // Getters and Setters
    public int getTrainId() { return trainId; }
    public void setTrainId(int trainId) { this.trainId = trainId; }

    public String getTrainNumber() { return trainNumber; }
    public void setTrainNumber(String trainNumber) { this.trainNumber = trainNumber; }

    public String getTrainName() { return trainName; }
    public void setTrainName(String trainName) { this.trainName = trainName; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public int getAcSeats() { return acSeats; }
    public void setAcSeats(int acSeats) { this.acSeats = acSeats; }

    public int getSleeperSeats() { return sleeperSeats; }
    public void setSleeperSeats(int sleeperSeats) { this.sleeperSeats = sleeperSeats; }

    public int getTatkalSeats() { return tatkalSeats; }
    public void setTatkalSeats(int tatkalSeats) { this.tatkalSeats = tatkalSeats; }

    public BigDecimal getFareAc() { return fareAc; }
    public void setFareAc(BigDecimal fareAc) { this.fareAc = fareAc; }

    public BigDecimal getFareSleeper() { return fareSleeper; }
    public void setFareSleeper(BigDecimal fareSleeper) { this.fareSleeper = fareSleeper; }

    public BigDecimal getFareGeneral() { return fareGeneral; }
    public void setFareGeneral(BigDecimal fareGeneral) { this.fareGeneral = fareGeneral; }

    public BigDecimal getTatkalSurcharge() { return tatkalSurcharge; }
    public void setTatkalSurcharge(BigDecimal tatkalSurcharge) { this.tatkalSurcharge = tatkalSurcharge; }

    public String getRunsOn() { return runsOn; }
    public void setRunsOn(String runsOn) { this.runsOn = runsOn; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
