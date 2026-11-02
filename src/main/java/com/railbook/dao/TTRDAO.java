package com.railbook.dao;

import com.railbook.model.AuditLog;
import com.railbook.model.Booking;
import com.railbook.model.TTRUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TTRDAO {

    public TTRUser findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM ttr_users WHERE username = ? AND status = 'ACTIVE'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TTRUser ttr = new TTRUser();
                    ttr.setTtrId(rs.getString("ttr_id"));
                    ttr.setName(rs.getString("name"));
                    ttr.setUsername(rs.getString("username"));
                    ttr.setBadgeNumber(rs.getString("badge_number"));
                    ttr.setDivision(rs.getString("division"));
                    ttr.setPasswordHash(rs.getString("password_hash"));
                    ttr.setStatus(rs.getString("status"));
                    ttr.setCreatedAt(rs.getTimestamp("created_at"));
                    return ttr;
                }
            }
        }
        return null;
    }

    public TTRUser findById(String ttrId) throws SQLException {
        String sql = "SELECT * FROM ttr_users WHERE ttr_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ttrId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TTRUser ttr = new TTRUser();
                    ttr.setTtrId(rs.getString("ttr_id"));
                    ttr.setName(rs.getString("name"));
                    ttr.setUsername(rs.getString("username"));
                    ttr.setBadgeNumber(rs.getString("badge_number"));
                    ttr.setDivision(rs.getString("division"));
                    ttr.setPasswordHash(rs.getString("password_hash"));
                    ttr.setStatus(rs.getString("status"));
                    ttr.setCreatedAt(rs.getTimestamp("created_at"));
                    return ttr;
                }
            }
        }
        return null;
    }

    public Booking verifyToken(String tokenInput) throws SQLException {
        // Strip URL prefix if TTR scanner passed complete verification URL
        String cleanToken = tokenInput.trim();
        if (cleanToken.contains("/ttr/verify/")) {
            cleanToken = cleanToken.substring(cleanToken.lastIndexOf("/ttr/verify/") + "/ttr/verify/".length());
        }

        String sql = "SELECT qr.*, b.pnr FROM qr_verifications qr " +
                     "JOIN bookings b ON qr.booking_id = b.booking_id " +
                     "WHERE qr.raw_token = ? OR qr.secure_token_hash = SHA2(?, 256)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cleanToken);
            ps.setString(2, cleanToken);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String bookingId = rs.getString("booking_id");
                    String qrStatus = rs.getString("status");
                    Timestamp expiresAt = rs.getTimestamp("expires_at");

                    BookingDAO bookingDAO = new BookingDAO();
                    Booking booking = bookingDAO.getBookingById(bookingId);

                    if (booking != null) {
                        // Check if QR is revoked or expired or ticket is cancelled
                        if (!"ACTIVE".equalsIgnoreCase(qrStatus)) {
                            booking.setStatus("QR REVOKED / INVALID");
                        } else if (expiresAt != null && expiresAt.before(new java.util.Date())) {
                            booking.setStatus("TICKET EXPIRED");
                        }
                        return booking;
                    }
                }
            }
        }
        return null;
    }

    public boolean logVerification(AuditLog log) throws SQLException {
        String sql = "INSERT INTO verification_audit_log (ttr_id, booking_id, result, action, ip_address, user_agent, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, log.getTtrId());
            ps.setString(2, log.getBookingId());
            ps.setString(3, log.getResult());
            ps.setString(4, log.getAction());
            ps.setString(5, log.getIpAddress() != null ? log.getIpAddress() : "127.0.0.1");
            ps.setString(6, log.getUserAgent() != null ? log.getUserAgent() : "RailBook TTR Terminal");
            ps.setString(7, log.getNotes());

            return ps.executeUpdate() > 0;
        }
    }

    public List<AuditLog> getAuditLogs(int limit) throws SQLException {
        List<AuditLog> list = new ArrayList<>();
        String sql = "SELECT a.*, t.name as ttr_name, b.pnr " +
                     "FROM verification_audit_log a " +
                     "LEFT JOIN ttr_users t ON a.ttr_id = t.ttr_id " +
                     "LEFT JOIN bookings b ON a.booking_id = b.booking_id " +
                     "ORDER BY a.verification_time DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 50);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditLog log = new AuditLog();
                    log.setLogId(rs.getInt("log_id"));
                    log.setTtrId(rs.getString("ttr_id"));
                    log.setBookingId(rs.getString("booking_id"));
                    log.setVerificationTime(rs.getTimestamp("verification_time"));
                    log.setResult(rs.getString("result"));
                    log.setAction(rs.getString("action"));
                    log.setIpAddress(rs.getString("ip_address"));
                    log.setUserAgent(rs.getString("user_agent"));
                    log.setNotes(rs.getString("notes"));
                    log.setTtrName(rs.getString("ttr_name"));
                    log.setPnr(rs.getString("pnr"));
                    list.add(log);
                }
            }
        }
        return list;
    }
}
