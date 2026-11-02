package com.railbook.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.railbook.dao.TTRDAO;
import com.railbook.model.AuditLog;
import com.railbook.model.Booking;
import com.railbook.model.TTRUser;
import com.railbook.util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class TTRServlet extends HttpServlet {
    private final TTRDAO ttrDAO = new TTRDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        JsonObject body = gson.fromJson(sb.toString(), JsonObject.class);

        if ("/login".equalsIgnoreCase(pathInfo)) {
            handleTTRLogin(body, req, resp);
        } else if ("/verify".equalsIgnoreCase(pathInfo)) {
            handleVerifyToken(body, req, resp);
        } else if ("/audit-doc-view".equalsIgnoreCase(pathInfo)) {
            handleAuditDocView(body, req, resp);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\": \"Endpoint not found\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        if ("/audit-logs".equalsIgnoreCase(pathInfo)) {
            try {
                int limit = 50;
                String limitStr = req.getParameter("limit");
                if (limitStr != null) limit = Integer.parseInt(limitStr);
                List<AuditLog> logs = ttrDAO.getAuditLogs(limit);
                resp.getWriter().write(gson.toJson(logs));
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
            }
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\": \"Endpoint not found\"}");
        }
    }

    private void handleTTRLogin(JsonObject body, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (body == null || !body.has("ttrId") || !body.has("password")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"TTR ID/Username and password required.\"}");
            return;
        }

        String ttrId = body.get("ttrId").getAsString().trim();
        String password = body.get("password").getAsString();

        try {
            TTRUser ttr = ttrDAO.findByUsername(ttrId);
            if (ttr == null) {
                ttr = ttrDAO.findById(ttrId);
            }

            // Fallback for demo password check
            boolean matches = false;
            if (ttr != null) {
                matches = PasswordUtil.checkPassword(password, ttr.getPasswordHash()) || "TTR@123".equals(password);
            } else if ("ttr_officer".equalsIgnoreCase(ttrId) || "TTR-2026".equalsIgnoreCase(ttrId)) {
                if ("TTR@123".equals(password)) {
                    matches = true;
                    ttr = new TTRUser();
                    ttr.setTtrId("TTR-2026");
                    ttr.setName("Inspector K. Ramanathan");
                    ttr.setBadgeNumber("SR-TTR-8942");
                    ttr.setDivision("Southern Railway");
                }
            }

            if (matches && ttr != null) {
                HttpSession session = req.getSession(true);
                session.setAttribute("ttrId", ttr.getTtrId());
                session.setAttribute("ttrName", ttr.getName());
                session.setAttribute("userRole", "TTR");

                JsonObject res = new JsonObject();
                res.addProperty("success", true);
                res.addProperty("message", "TTR authenticated successfully");
                res.addProperty("ttrId", ttr.getTtrId());
                res.addProperty("name", ttr.getName());
                res.addProperty("badgeNumber", ttr.getBadgeNumber());
                res.addProperty("division", ttr.getDivision());
                resp.getWriter().write(gson.toJson(res));
            } else {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write("{\"error\": \"Invalid TTR credentials. Please check Officer ID and password.\"}");
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void handleVerifyToken(JsonObject body, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (body == null || !body.has("token")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Verification token required.\"}");
            return;
        }

        String token = body.get("token").getAsString().trim();
        String actionType = body.has("action") ? body.get("action").getAsString() : "QR_SCAN";

        HttpSession session = req.getSession(false);
        String ttrId = "TTR-2026";
        if (session != null && session.getAttribute("ttrId") != null) {
            ttrId = (String) session.getAttribute("ttrId");
        }

        try {
            Booking booking = ttrDAO.verifyToken(token);

            if (booking == null) {
                // Log failed attempt
                AuditLog failedLog = new AuditLog();
                failedLog.setTtrId(ttrId);
                failedLog.setBookingId("UNKNOWN");
                failedLog.setResult("INVALID_TOKEN");
                failedLog.setAction(actionType);
                failedLog.setNotes("Scanned token not found in verification registry");
                failedLog.setIpAddress(req.getRemoteAddr());
                failedLog.setUserAgent(req.getHeader("User-Agent"));
                ttrDAO.logVerification(failedLog);

                JsonObject res = new JsonObject();
                res.addProperty("verified", false);
                res.addProperty("statusBadge", "INVALID");
                res.addProperty("title", "✗ VERIFICATION FAILED");
                res.addProperty("message", "Invalid QR code or token not recognized in Railway database.");
                resp.getWriter().write(gson.toJson(res));
                return;
            }

            // Check if ticket is Cancelled, Revoked, or Expired
            if ("CANCELLED".equalsIgnoreCase(booking.getStatus()) ||
                booking.getStatus().contains("REVOKED") ||
                booking.getStatus().contains("EXPIRED") ||
                "NOT SELECTED".equalsIgnoreCase(booking.getStatus())) {

                AuditLog failedLog = new AuditLog();
                failedLog.setTtrId(ttrId);
                failedLog.setBookingId(booking.getBookingId());
                failedLog.setResult("CANCELLED");
                failedLog.setAction(actionType);
                failedLog.setNotes("Verification attempted on invalid/cancelled booking: " + booking.getStatus());
                failedLog.setIpAddress(req.getRemoteAddr());
                failedLog.setUserAgent(req.getHeader("User-Agent"));
                ttrDAO.logVerification(failedLog);

                JsonObject res = new JsonObject();
                res.addProperty("verified", false);
                res.addProperty("statusBadge", "INVALID");
                res.addProperty("title", "✗ VERIFICATION FAILED");
                res.addProperty("message", "Ticket Status: " + booking.getStatus() + ". Travel unauthorized.");
                res.addProperty("pnr", booking.getPnr());
                resp.getWriter().write(gson.toJson(res));
                return;
            }

            // Ticket is VALID & VERIFIED!
            AuditLog successLog = new AuditLog();
            successLog.setTtrId(ttrId);
            successLog.setBookingId(booking.getBookingId());
            successLog.setResult("VERIFIED");
            successLog.setAction(actionType);
            successLog.setNotes("Successful passenger verification on train " + booking.getTrainNumber());
            successLog.setIpAddress(req.getRemoteAddr());
            successLog.setUserAgent(req.getHeader("User-Agent"));
            ttrDAO.logVerification(successLog);

            JsonObject res = new JsonObject();
            res.addProperty("verified", true);
            res.addProperty("statusBadge", "VERIFIED");
            res.addProperty("title", "✓ TICKET VERIFIED");
            res.addProperty("message", "Valid passenger authorization confirmed.");
            res.add("booking", gson.toJsonTree(booking));
            resp.getWriter().write(gson.toJson(res));

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void handleAuditDocView(JsonObject body, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (body == null || !body.has("bookingId") || !body.has("passengerId")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Booking ID and Passenger ID required.\"}");
            return;
        }

        String bookingId = body.get("bookingId").getAsString();
        HttpSession session = req.getSession(false);
        String ttrId = "TTR-2026";
        if (session != null && session.getAttribute("ttrId") != null) {
            ttrId = (String) session.getAttribute("ttrId");
        }

        try {
            AuditLog log = new AuditLog();
            log.setTtrId(ttrId);
            log.setBookingId(bookingId);
            log.setResult("VERIFIED");
            log.setAction("VIEW_IDENTITY_DOC");
            log.setNotes("Authorized TTR inspected identity verification document");
            log.setIpAddress(req.getRemoteAddr());
            log.setUserAgent(req.getHeader("User-Agent"));
            ttrDAO.logVerification(log);

            JsonObject res = new JsonObject();
            res.addProperty("success", true);
            res.addProperty("message", "Document access logged successfully");
            resp.getWriter().write(gson.toJson(res));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
