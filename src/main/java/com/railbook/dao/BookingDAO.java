package com.railbook.dao;

import com.railbook.model.Booking;
import com.railbook.model.IdentityDocument;
import com.railbook.model.Passenger;
import com.railbook.model.QRVerification;

import java.sql.*;
import java.util.*;

public class BookingDAO {

    public boolean createBooking(Booking booking, List<Passenger> passengers, QRVerification qr) throws SQLException {
        String sqlBooking = "INSERT INTO bookings (booking_id, pnr, user_id, train_id, journey_date, source, destination, travel_class, quota, fare, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlPassenger = "INSERT INTO passengers (booking_id, name, age, gender, berth_preference, nationality, mobile_number, coach, seat, status, is_senior_citizen) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlDoc = "INSERT INTO identity_documents (passenger_id, document_type, file_name, encrypted_file_reference, masked_identifier, verification_status) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlQr = "INSERT INTO qr_verifications (booking_id, secure_token_hash, raw_token, verification_url, expires_at, status) VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Insert Booking
            try (PreparedStatement ps = conn.prepareStatement(sqlBooking)) {
                ps.setString(1, booking.getBookingId());
                ps.setString(2, booking.getPnr());
                ps.setInt(3, booking.getUserId());
                ps.setInt(4, booking.getTrainId());
                ps.setDate(5, booking.getJourneyDate());
                ps.setString(6, booking.getSource());
                ps.setString(7, booking.getDestination());
                ps.setString(8, booking.getTravelClass());
                ps.setString(9, booking.getQuota());
                ps.setBigDecimal(10, booking.getFare());
                ps.setString(11, booking.getStatus());
                ps.executeUpdate();
            }

            // 2. Insert Passengers and Identity Documents
            for (Passenger p : passengers) {
                int passengerId = 0;
                try (PreparedStatement ps = conn.prepareStatement(sqlPassenger, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, booking.getBookingId());
                    ps.setString(2, p.getName());
                    ps.setInt(3, p.getAge());
                    ps.setString(4, p.getGender());
                    ps.setString(5, p.getBerthPreference());
                    ps.setString(6, p.getNationality() != null ? p.getNationality() : "Indian");
                    ps.setString(7, p.getMobileNumber());
                    ps.setString(8, p.getCoach());
                    ps.setString(9, p.getSeat());
                    ps.setString(10, p.getStatus() != null ? p.getStatus() : booking.getStatus());
                    ps.setBoolean(11, p.isSeniorCitizen());
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            passengerId = rs.getInt(1);
                            p.setPassengerId(passengerId);
                        }
                    }
                }

                // If passenger has an identity document, insert it
                if (passengerId > 0 && p.getIdentityDocument() != null) {
                    IdentityDocument doc = p.getIdentityDocument();
                    try (PreparedStatement psDoc = conn.prepareStatement(sqlDoc)) {
                        psDoc.setInt(1, passengerId);
                        psDoc.setString(2, doc.getDocumentType() != null ? doc.getDocumentType() : "Aadhaar Card (Demo)");
                        psDoc.setString(3, doc.getFileName() != null ? doc.getFileName() : "identity_proof.pdf");
                        psDoc.setString(4, doc.getEncryptedFileReference() != null ? doc.getEncryptedFileReference() : "uploads/demo.pdf");
                        psDoc.setString(5, doc.getMaskedIdentifier() != null ? doc.getMaskedIdentifier() : "XXXX XXXX 1234");
                        psDoc.setString(6, "VERIFIED");
                        psDoc.executeUpdate();
                    }
                }
            }

            // 3. Insert Secure QR Verification
            try (PreparedStatement ps = conn.prepareStatement(sqlQr)) {
                ps.setString(1, booking.getBookingId());
                ps.setString(2, qr.getSecureTokenHash());
                ps.setString(3, qr.getRawToken());
                ps.setString(4, qr.getVerificationUrl());
                ps.setTimestamp(5, qr.getExpiresAt());
                ps.setString(6, qr.getStatus() != null ? qr.getStatus() : "ACTIVE");
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException ex) {
            if (conn != null) {
                conn.rollback();
            }
            throw ex;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public Booking getBookingByPNR(String pnr) throws SQLException {
        String sql = "SELECT b.*, t.train_number, t.train_name, u.name as user_name, u.phone as user_phone, u.email as user_email " +
                     "FROM bookings b " +
                     "JOIN trains t ON b.train_id = t.train_id " +
                     "JOIN users u ON b.user_id = u.user_id " +
                     "WHERE b.pnr = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pnr.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Booking b = mapRowToBooking(rs);
                    b.setPassengers(getPassengersForBooking(conn, b.getBookingId()));
                    b.setQrVerification(getQRForBooking(conn, b.getBookingId()));
                    return b;
                }
            }
        }
        return null;
    }

    public Booking getBookingById(String bookingId) throws SQLException {
        String sql = "SELECT b.*, t.train_number, t.train_name, u.name as user_name, u.phone as user_phone, u.email as user_email " +
                     "FROM bookings b " +
                     "JOIN trains t ON b.train_id = t.train_id " +
                     "JOIN users u ON b.user_id = u.user_id " +
                     "WHERE b.booking_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bookingId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Booking b = mapRowToBooking(rs);
                    b.setPassengers(getPassengersForBooking(conn, b.getBookingId()));
                    b.setQrVerification(getQRForBooking(conn, b.getBookingId()));
                    return b;
                }
            }
        }
        return null;
    }

    public List<Booking> getBookingsByUserId(int userId) throws SQLException {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, t.train_number, t.train_name, u.name as user_name, u.phone as user_phone, u.email as user_email " +
                     "FROM bookings b " +
                     "JOIN trains t ON b.train_id = t.train_id " +
                     "JOIN users u ON b.user_id = u.user_id " +
                     "WHERE b.user_id = ? " +
                     "ORDER BY b.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Booking b = mapRowToBooking(rs);
                    b.setPassengers(getPassengersForBooking(conn, b.getBookingId()));
                    b.setQrVerification(getQRForBooking(conn, b.getBookingId()));
                    list.add(b);
                }
            }
        }
        return list;
    }

    public List<Booking> getAllBookings() throws SQLException {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, t.train_number, t.train_name, u.name as user_name, u.phone as user_phone, u.email as user_email " +
                     "FROM bookings b " +
                     "JOIN trains t ON b.train_id = t.train_id " +
                     "JOIN users u ON b.user_id = u.user_id " +
                     "ORDER BY b.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Booking b = mapRowToBooking(rs);
                b.setPassengers(getPassengersForBooking(conn, b.getBookingId()));
                b.setQrVerification(getQRForBooking(conn, b.getBookingId()));
                list.add(b);
            }
        }
        return list;
    }

    public boolean updateBookingStatus(String bookingId, String newStatus) throws SQLException {
        String sql1 = "UPDATE bookings SET status = ? WHERE booking_id = ?";
        String sql2 = "UPDATE passengers SET status = ? WHERE booking_id = ?";
        String sql3 = "UPDATE qr_verifications SET status = ? WHERE booking_id = ?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps1 = conn.prepareStatement(sql1)) {
                ps1.setString(1, newStatus);
                ps1.setString(2, bookingId);
                ps1.executeUpdate();
            }

            try (PreparedStatement ps2 = conn.prepareStatement(sql2)) {
                ps2.setString(1, newStatus);
                ps2.setString(2, bookingId);
                ps2.executeUpdate();
            }

            if ("CANCELLED".equalsIgnoreCase(newStatus) || "NOT SELECTED".equalsIgnoreCase(newStatus)) {
                try (PreparedStatement ps3 = conn.prepareStatement(sql3)) {
                    ps3.setString(1, "REVOKED");
                    ps3.setString(2, bookingId);
                    ps3.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException ex) {
            if (conn != null) conn.rollback();
            throw ex;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public Map<String, Integer> getDashboardStats() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalBookings", 0);
        stats.put("confirmedTickets", 0);
        stats.put("waitingList", 0);
        stats.put("notSelected", 0);
        stats.put("totalPassengers", 0);
        stats.put("todayBookings", 0);

        String sql = "SELECT " +
                     "COUNT(*) as total, " +
                     "SUM(CASE WHEN status = 'CONFIRMED' THEN 1 ELSE 0 END) as confirmed, " +
                     "SUM(CASE WHEN status = 'WAITING LIST' THEN 1 ELSE 0 END) as waiting, " +
                     "SUM(CASE WHEN status = 'NOT SELECTED' THEN 1 ELSE 0 END) as not_selected, " +
                     "SUM(CASE WHEN DATE(created_at) = CURDATE() THEN 1 ELSE 0 END) as today " +
                     "FROM bookings";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                stats.put("totalBookings", rs.getInt("total"));
                stats.put("confirmedTickets", rs.getInt("confirmed"));
                stats.put("waitingList", rs.getInt("waiting"));
                stats.put("notSelected", rs.getInt("not_selected"));
                stats.put("todayBookings", rs.getInt("today"));
            }
        }

        String sqlPassengers = "SELECT COUNT(*) FROM passengers";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlPassengers)) {
            if (rs.next()) {
                stats.put("totalPassengers", rs.getInt(1));
            }
        }

        return stats;
    }

    private List<Passenger> getPassengersForBooking(Connection conn, String bookingId) throws SQLException {
        List<Passenger> list = new ArrayList<>();
        String sql = "SELECT p.*, d.document_id, d.document_type, d.file_name, d.encrypted_file_reference, d.masked_identifier, d.verification_status " +
                     "FROM passengers p " +
                     "LEFT JOIN identity_documents d ON p.passenger_id = d.passenger_id " +
                     "WHERE p.booking_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Passenger p = new Passenger();
                    p.setPassengerId(rs.getInt("passenger_id"));
                    p.setBookingId(rs.getString("booking_id"));
                    p.setName(rs.getString("name"));
                    p.setAge(rs.getInt("age"));
                    p.setGender(rs.getString("gender"));
                    p.setBerthPreference(rs.getString("berth_preference"));
                    p.setNationality(rs.getString("nationality"));
                    p.setMobileNumber(rs.getString("mobile_number"));
                    p.setCoach(rs.getString("coach"));
                    p.setSeat(rs.getString("seat"));
                    p.setStatus(rs.getString("status"));
                    p.setSeniorCitizen(rs.getBoolean("is_senior_citizen"));
                    p.setCreatedAt(rs.getTimestamp("created_at"));

                    int docId = rs.getInt("document_id");
                    if (docId > 0) {
                        IdentityDocument doc = new IdentityDocument();
                        doc.setDocumentId(docId);
                        doc.setPassengerId(p.getPassengerId());
                        doc.setDocumentType(rs.getString("document_type"));
                        doc.setFileName(rs.getString("file_name"));
                        doc.setEncryptedFileReference(rs.getString("encrypted_file_reference"));
                        doc.setMaskedIdentifier(rs.getString("masked_identifier"));
                        doc.setVerificationStatus(rs.getString("verification_status"));
                        p.setIdentityDocument(doc);
                    }
                    list.add(p);
                }
            }
        }
        return list;
    }

    private QRVerification getQRForBooking(Connection conn, String bookingId) throws SQLException {
        String sql = "SELECT * FROM qr_verifications WHERE booking_id = ? ORDER BY qr_id DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    QRVerification qr = new QRVerification();
                    qr.setQrId(rs.getInt("qr_id"));
                    qr.setBookingId(rs.getString("booking_id"));
                    qr.setSecureTokenHash(rs.getString("secure_token_hash"));
                    qr.setRawToken(rs.getString("raw_token"));
                    qr.setVerificationUrl(rs.getString("verification_url"));
                    qr.setExpiresAt(rs.getTimestamp("expires_at"));
                    qr.setStatus(rs.getString("status"));
                    qr.setCreatedAt(rs.getTimestamp("created_at"));
                    return qr;
                }
            }
        }
        return null;
    }

    private Booking mapRowToBooking(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setBookingId(rs.getString("booking_id"));
        b.setPnr(rs.getString("pnr"));
        b.setUserId(rs.getInt("user_id"));
        b.setTrainId(rs.getInt("train_id"));
        b.setJourneyDate(rs.getDate("journey_date"));
        b.setSource(rs.getString("source"));
        b.setDestination(rs.getString("destination"));
        b.setTravelClass(rs.getString("travel_class"));
        b.setQuota(rs.getString("quota"));
        b.setFare(rs.getBigDecimal("fare"));
        b.setStatus(rs.getString("status"));
        b.setCreatedAt(rs.getTimestamp("created_at"));

        b.setTrainNumber(rs.getString("train_number"));
        b.setTrainName(rs.getString("train_name"));
        b.setUserName(rs.getString("user_name"));
        b.setUserPhone(rs.getString("user_phone"));
        b.setUserEmail(rs.getString("user_email"));
        return b;
    }
}
