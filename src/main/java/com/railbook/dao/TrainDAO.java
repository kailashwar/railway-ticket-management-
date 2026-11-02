package com.railbook.dao;

import com.railbook.model.Station;
import com.railbook.model.Train;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TrainDAO {

    public List<Train> searchTrains(String source, String destination, String travelClass, String quota, boolean onlyAvailable) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM trains WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (source != null && !source.trim().isEmpty()) {
            sql.append(" AND LOWER(source) LIKE ?");
            params.add("%" + source.trim().toLowerCase() + "%");
        }
        if (destination != null && !destination.trim().isEmpty()) {
            sql.append(" AND LOWER(destination) LIKE ?");
            params.add("%" + destination.trim().toLowerCase() + "%");
        }
        if (onlyAvailable) {
            sql.append(" AND available_seats > 0");
        }
        sql.append(" ORDER BY departure_time ASC");

        List<Train> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTrain(rs));
                }
            }
        }
        return list;
    }

    public List<Train> getAllTrains() throws SQLException {
        List<Train> list = new ArrayList<>();
        String sql = "SELECT * FROM trains ORDER BY train_number ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRowToTrain(rs));
            }
        }
        return list;
    }

    public Train getTrainById(int trainId) throws SQLException {
        String sql = "SELECT * FROM trains WHERE train_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trainId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTrain(rs);
                }
            }
        }
        return null;
    }

    public Train getTrainByNumber(String trainNumber) throws SQLException {
        String sql = "SELECT * FROM trains WHERE train_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trainNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTrain(rs);
                }
            }
        }
        return null;
    }

    public boolean addTrain(Train train) throws SQLException {
        String sql = "INSERT INTO trains (train_number, train_name, source, destination, departure_time, arrival_time, duration, total_seats, available_seats, ac_seats, sleeper_seats, tatkal_seats, fare_ac, fare_sleeper, fare_general, tatkal_surcharge, runs_on) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, train.getTrainNumber());
            ps.setString(2, train.getTrainName());
            ps.setString(3, train.getSource());
            ps.setString(4, train.getDestination());
            ps.setString(5, train.getDepartureTime());
            ps.setString(6, train.getArrivalTime());
            ps.setString(7, train.getDuration() != null ? train.getDuration() : "6h 00m");
            ps.setInt(8, train.getTotalSeats());
            ps.setInt(9, train.getAvailableSeats());
            ps.setInt(10, train.getAcSeats());
            ps.setInt(11, train.getSleeperSeats());
            ps.setInt(12, train.getTatkalSeats());
            ps.setBigDecimal(13, train.getFareAc());
            ps.setBigDecimal(14, train.getFareSleeper());
            ps.setBigDecimal(15, train.getFareGeneral());
            ps.setBigDecimal(16, train.getTatkalSurcharge());
            ps.setString(17, train.getRunsOn() != null ? train.getRunsOn() : "Daily");

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        train.setTrainId(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean updateTrain(Train train) throws SQLException {
        String sql = "UPDATE trains SET train_name = ?, source = ?, destination = ?, departure_time = ?, arrival_time = ?, total_seats = ?, available_seats = ?, ac_seats = ?, sleeper_seats = ?, tatkal_seats = ?, fare_ac = ?, fare_sleeper = ? WHERE train_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, train.getTrainName());
            ps.setString(2, train.getSource());
            ps.setString(3, train.getDestination());
            ps.setString(4, train.getDepartureTime());
            ps.setString(5, train.getArrivalTime());
            ps.setInt(6, train.getTotalSeats());
            ps.setInt(7, train.getAvailableSeats());
            ps.setInt(8, train.getAcSeats());
            ps.setInt(9, train.getSleeperSeats());
            ps.setInt(10, train.getTatkalSeats());
            ps.setBigDecimal(11, train.getFareAc());
            ps.setBigDecimal(12, train.getFareSleeper());
            ps.setInt(13, train.getTrainId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteTrain(int trainId) throws SQLException {
        String sql = "DELETE FROM trains WHERE train_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trainId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Station> getAllStations() throws SQLException {
        List<Station> stations = new ArrayList<>();
        String sql = "SELECT * FROM stations ORDER BY station_name ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Station s = new Station();
                s.setStationId(rs.getInt("station_id"));
                s.setStationCode(rs.getString("station_code"));
                s.setStationName(rs.getString("station_name"));
                s.setCity(rs.getString("city"));
                s.setState(rs.getString("state"));
                stations.add(s);
            }
        }
        return stations;
    }

    private Train mapRowToTrain(ResultSet rs) throws SQLException {
        Train t = new Train();
        t.setTrainId(rs.getInt("train_id"));
        t.setTrainNumber(rs.getString("train_number"));
        t.setTrainName(rs.getString("train_name"));
        t.setSource(rs.getString("source"));
        t.setDestination(rs.getString("destination"));
        t.setDepartureTime(rs.getString("departure_time"));
        t.setArrivalTime(rs.getString("arrival_time"));
        t.setDuration(rs.getString("duration"));
        t.setTotalSeats(rs.getInt("total_seats"));
        t.setAvailableSeats(rs.getInt("available_seats"));
        t.setAcSeats(rs.getInt("ac_seats"));
        t.setSleeperSeats(rs.getInt("sleeper_seats"));
        t.setTatkalSeats(rs.getInt("tatkal_seats"));
        t.setFareAc(rs.getBigDecimal("fare_ac"));
        t.setFareSleeper(rs.getBigDecimal("fare_sleeper"));
        t.setFareGeneral(rs.getBigDecimal("fare_general"));
        t.setTatkalSurcharge(rs.getBigDecimal("tatkal_surcharge"));
        t.setRunsOn(rs.getString("runs_on"));
        t.setCreatedAt(rs.getTimestamp("created_at"));
        return t;
    }
}
