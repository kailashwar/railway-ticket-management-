package com.railbook.controller;

import com.google.gson.JsonObject;
import com.railbook.dao.DBConnection;
import com.railbook.dao.TTRDAO;
import com.railbook.model.AuditLog;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class IdentityDocumentServlet extends HttpServlet {
    private final TTRDAO ttrDAO = new TTRDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        // Ensure only authorized TTR or Admin can access
        if (session == null || (!"TTR".equals(session.getAttribute("userRole")) && !"ADMIN".equals(session.getAttribute("userRole")))) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\": \"Access denied. Only authorized TTR or Admin officers can view identity records.\"}");
            return;
        }

        String docIdStr = req.getParameter("id");
        if (docIdStr == null || docIdStr.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Document ID required\"}");
            return;
        }

        try {
            int docId = Integer.parseInt(docIdStr);
            String sql = "SELECT d.*, p.booking_id, p.name FROM identity_documents d JOIN passengers p ON d.passenger_id = p.passenger_id WHERE d.document_id = ?";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, docId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String bookingId = rs.getString("booking_id");
                        String passengerName = rs.getString("name");
                        String maskedIdentifier = rs.getString("masked_identifier");
                        String docType = rs.getString("document_type");

                        // Log this document view action in audit log
                        String ttrId = (String) session.getAttribute("ttrId");
                        if (ttrId == null) ttrId = "OFFICER-" + session.getAttribute("userId");

                        AuditLog log = new AuditLog();
                        log.setTtrId(ttrId);
                        log.setBookingId(bookingId);
                        log.setResult("VERIFIED");
                        log.setAction("VIEW_IDENTITY_DOC");
                        log.setNotes("Officer viewed demo identity document for passenger " + passengerName);
                        log.setIpAddress(req.getRemoteAddr());
                        log.setUserAgent(req.getHeader("User-Agent"));
                        ttrDAO.logVerification(log);

                        // Return controlled preview response
                        resp.setContentType("application/json");
                        JsonObject res = new JsonObject();
                        res.addProperty("documentId", docId);
                        res.addProperty("documentType", docType);
                        res.addProperty("passengerName", passengerName);
                        res.addProperty("maskedIdentifier", maskedIdentifier);
                        res.addProperty("verificationStatus", "VERIFIED");
                        res.addProperty("notice", "Demo Identity Verification – Test Document Only. Logged for security.");
                        res.addProperty("documentPreviewSvg", generateSecureBadgeSvg(maskedIdentifier, passengerName));
                        resp.getWriter().write(res.toString());
                    } else {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().write("{\"error\": \"Document not found\"}");
                    }
                }
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private String generateSecureBadgeSvg(String maskedId, String name) {
        return "<svg width='380' height='220' xmlns='http://www.w3.org/2000/svg'>" +
               "<rect width='100%' height='100%' rx='12' fill='#1a365d'/>" +
               "<rect x='10' y='10' width='360' height='200' rx='8' fill='#ffffff'/>" +
               "<text x='30' y='45' font-family='Arial, sans-serif' font-size='16' font-weight='bold' fill='#0d3b66'>GOVERNMENT IDENTITY DEMO CARD</text>" +
               "<text x='30' y='65' font-family='Arial, sans-serif' font-size='11' fill='#e53e3e'>STUDENT PROJECT ONLY - TEST RECORD</text>" +
               "<line x1='30' y1='75' x2='350' y2='75' stroke='#cbd5e1' stroke-width='1'/>" +
               "<rect x='30' y='95' width='60' height='75' rx='4' fill='#e2e8f0'/>" +
               "<text x='48' y='140' font-family='Arial' font-size='24' fill='#718096'>ID</text>" +
               "<text x='105' y='110' font-family='Arial' font-size='12' fill='#718096'>Holder Name:</text>" +
               "<text x='105' y='128' font-family='Arial' font-size='14' font-weight='bold' fill='#1a202c'>" + name + "</text>" +
               "<text x='105' y='150' font-family='Arial' font-size='12' fill='#718096'>Identifier Number:</text>" +
               "<text x='105' y='168' font-family='Arial' font-size='16' font-weight='bold' fill='#2b6cb0' letter-spacing='2'>" + maskedId + "</text>" +
               "<rect x='30' y='185' width='320' height='15' fill='#e6fffa' rx='3'/>" +
               "<text x='110' y='196' font-family='Arial' font-size='10' fill='#234e52' font-weight='bold'>✓ VERIFIED SECURE DEMO DOCUMENT</text>" +
               "</svg>";
    }
}
