package com.railbook.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.railbook.dao.BookingDAO;
import com.railbook.dao.TrainDAO;
import com.railbook.model.*;
import com.railbook.service.BerthAllocationService;
import com.railbook.service.QRService;
import com.railbook.util.TokenUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookingServlet extends HttpServlet {
    private final BookingDAO bookingDAO = new BookingDAO();
    private final TrainDAO trainDAO = new TrainDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        if ("/create".equalsIgnoreCase(pathInfo)) {
            handleCreateBooking(req, resp);
        } else if ("/cancel".equalsIgnoreCase(pathInfo)) {
            handleCancelBooking(req, resp);
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

        try {
            if ("/my".equalsIgnoreCase(pathInfo)) {
                HttpSession session = req.getSession(false);
                int userId = 2; // Default demo passenger
                if (session != null && session.getAttribute("userId") != null) {
                    userId = (Integer) session.getAttribute("userId");
                }
                List<Booking> bookings = bookingDAO.getBookingsByUserId(userId);
                resp.getWriter().write(gson.toJson(bookings));
            } else if (pathInfo != null && pathInfo.length() > 1) {
                // Fetch by PNR or Booking ID: /api/bookings/4567891234
                String pnrOrId = pathInfo.substring(1);
                Booking booking = bookingDAO.getBookingByPNR(pnrOrId);
                if (booking == null) {
                    booking = bookingDAO.getBookingById(pnrOrId);
                }

                if (booking != null) {
                    // Enrich with base64 QR Code
                    if (booking.getQrVerification() != null) {
                        String qrBase64 = QRService.generateQRCodeBase64(
                                booking.getQrVerification().getVerificationUrl(), 260, 260);
                        JsonObject obj = gson.toJsonTree(booking).getAsJsonObject();
                        obj.addProperty("qrCodeBase64", qrBase64);
                        resp.getWriter().write(gson.toJson(obj));
                        return;
                    }
                    resp.getWriter().write(gson.toJson(booking));
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"error\": \"Booking not found for identifier: " + pnrOrId + "\"}");
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Missing booking identifier\"}");
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void handleCreateBooking(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        JsonObject body = gson.fromJson(sb.toString(), JsonObject.class);

        if (body == null || !body.has("trainId") || !body.has("journeyDate") || !body.has("passengers")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Missing required booking parameters.\"}");
            return;
        }

        try {
            int trainId = body.get("trainId").getAsInt();
            Train train = trainDAO.getTrainById(trainId);
            if (train == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"Selected train not found.\"}");
                return;
            }

            HttpSession session = req.getSession(false);
            int userId = 2; // Default demo passenger
            if (session != null && session.getAttribute("userId") != null) {
                userId = (Integer) session.getAttribute("userId");
            }

            String journeyDateStr = body.get("journeyDate").getAsString();
            String travelClass = body.has("travelClass") ? body.get("travelClass").getAsString() : "Sleeper";
            String quota = body.has("quota") ? body.get("quota").getAsString() : "General";

            // Parse passengers
            JsonArray passArr = body.getAsJsonArray("passengers");
            List<Passenger> passengers = new ArrayList<>();
            List<String> seniorAlerts = new ArrayList<>();

            for (JsonElement el : passArr) {
                JsonObject pObj = el.getAsJsonObject();
                Passenger p = new Passenger();
                p.setName(pObj.get("name").getAsString());
                int age = pObj.get("age").getAsInt();
                p.setAge(age);
                p.setGender(pObj.get("gender").getAsString());
                p.setBerthPreference(pObj.has("berthPreference") ? pObj.get("berthPreference").getAsString() : "No Preference");
                p.setNationality(pObj.has("nationality") ? pObj.get("nationality").getAsString() : "Indian");
                p.setMobileNumber(pObj.has("mobileNumber") ? pObj.get("mobileNumber").getAsString() : "");

                // Senior Citizen check: Age > 60 automatically prefers lower berth
                if (age > 60) {
                    seniorAlerts.add(p.getName() + " is above 60 years. Lower berth has been automatically preferred.");
                }

                // Mock identity document
                IdentityDocument doc = new IdentityDocument();
                doc.setDocumentType("Aadhaar Card (Demo)");
                doc.setFileName("demo_id_proof_" + System.currentTimeMillis() + ".pdf");
                doc.setEncryptedFileReference("uploads/demo_doc.pdf");
                String rawAadhaar = pObj.has("aadhaarNumber") ? pObj.get("aadhaarNumber").getAsString() : "987654321234";
                doc.setMaskedIdentifier(TokenUtil.maskIdentifier(rawAadhaar));
                doc.setVerificationStatus("VERIFIED");
                p.setIdentityDocument(doc);

                passengers.add(p);
            }

            // Allocate Coach and Berths using Railway Allocation Engine
            BerthAllocationService.allocateBerths(passengers, travelClass);

            // Compute Fare
            BigDecimal baseFare = travelClass.contains("AC") ? train.getFareAc() : train.getFareSleeper();
            if ("Tatkal".equalsIgnoreCase(quota)) {
                baseFare = baseFare.add(train.getTatkalSurcharge());
            }
            BigDecimal totalFare = baseFare.multiply(new BigDecimal(passengers.size()));

            // Generate PNR and Booking ID
            String pnr = TokenUtil.generatePNR();
            String bookingId = TokenUtil.generateBookingId();

            Booking booking = new Booking();
            booking.setBookingId(bookingId);
            booking.setPnr(pnr);
            booking.setUserId(userId);
            booking.setTrainId(trainId);
            booking.setJourneyDate(Date.valueOf(journeyDateStr));
            booking.setSource(train.getSource());
            booking.setDestination(train.getDestination());
            booking.setTravelClass(travelClass);
            booking.setQuota(quota);
            booking.setFare(totalFare);
            booking.setStatus("WAITING LIST");

            // Generate Secure QR Verification (contains ONLY verification token or URL, never Aadhaar!)
            QRVerification qr = QRService.createQRVerification(bookingId, pnr);

            // Save in database
            boolean success = bookingDAO.createBooking(booking, passengers, qr);

            if (success) {
                String qrBase64 = QRService.generateQRCodeBase64(qr.getVerificationUrl(), 260, 260);

                JsonObject responseObj = new JsonObject();
                responseObj.addProperty("success", true);
                responseObj.addProperty("message", "Booking Submitted! Awaiting Admin Confirmation.");
                responseObj.addProperty("pnr", pnr);
                responseObj.addProperty("bookingId", bookingId);
                responseObj.addProperty("status", "WAITING LIST");
                responseObj.addProperty("totalFare", totalFare);
                responseObj.addProperty("qrVerificationUrl", qr.getVerificationUrl());
                responseObj.addProperty("qrCodeBase64", qrBase64);
                responseObj.add("seniorCitizenAlerts", gson.toJsonTree(seniorAlerts));

                resp.getWriter().write(gson.toJson(responseObj));
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\": \"Failed to create booking in database.\"}");
            }

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void handleCancelBooking(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        JsonObject body = gson.fromJson(sb.toString(), JsonObject.class);

        if (body == null || !body.has("bookingId")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Booking ID required.\"}");
            return;
        }

        String bookingId = body.get("bookingId").getAsString();
        try {
            boolean updated = bookingDAO.updateBookingStatus(bookingId, "CANCELLED");
            if (updated) {
                resp.getWriter().write("{\"success\": true, \"message\": \"Booking cancelled successfully. Secure QR has been revoked.\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"Booking not found or could not be cancelled.\"}");
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
