package com.railbook.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.railbook.dao.BookingDAO;
import com.railbook.model.Booking;
import com.railbook.service.QRService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

public class PNRServlet extends HttpServlet {
    private final BookingDAO bookingDAO = new BookingDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"PNR number required\"}");
            return;
        }

        String pnr = pathInfo.substring(1).trim();

        try {
            Booking booking = bookingDAO.getBookingByPNR(pnr);
            if (booking != null) {
                // Enrich with Base64 QR for verification
                JsonObject obj = gson.toJsonTree(booking).getAsJsonObject();
                if (booking.getQrVerification() != null) {
                    String qrBase64 = QRService.generateQRCodeBase64(
                            booking.getQrVerification().getVerificationUrl(), 240, 240);
                    obj.addProperty("qrCodeBase64", qrBase64);
                }
                resp.getWriter().write(gson.toJson(obj));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"No booking found for PNR: " + pnr + ". Please check the 10-digit number.\"}");
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
