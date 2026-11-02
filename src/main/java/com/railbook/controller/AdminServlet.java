package com.railbook.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.railbook.dao.BookingDAO;
import com.railbook.dao.TrainDAO;
import com.railbook.dao.UserDAO;
import com.railbook.model.Booking;
import com.railbook.model.Train;
import com.railbook.model.User;
import com.railbook.util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class AdminServlet extends HttpServlet {
    private final BookingDAO bookingDAO = new BookingDAO();
    private final TrainDAO trainDAO = new TrainDAO();
    private final UserDAO userDAO = new UserDAO();
    private final Gson gson = new Gson();

    private static final String DEFAULT_ADMIN_EMAIL = "kailashwar24@gmail.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "Kailash@24";

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
            handleAdminLogin(body, req, resp);
        } else if ("/decide".equalsIgnoreCase(pathInfo)) {
            handleTicketDecision(body, req, resp);
        } else if ("/trains/add".equalsIgnoreCase(pathInfo)) {
            handleAddTrain(body, req, resp);
        } else if ("/trains/update".equalsIgnoreCase(pathInfo)) {
            handleUpdateTrain(body, req, resp);
        } else if ("/trains/delete".equalsIgnoreCase(pathInfo)) {
            handleDeleteTrain(body, req, resp);
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
            if ("/stats".equalsIgnoreCase(pathInfo)) {
                Map<String, Integer> stats = bookingDAO.getDashboardStats();
                resp.getWriter().write(gson.toJson(stats));
            } else if ("/bookings".equalsIgnoreCase(pathInfo)) {
                List<Booking> bookings = bookingDAO.getAllBookings();
                resp.getWriter().write(gson.toJson(bookings));
            } else if ("/trains".equalsIgnoreCase(pathInfo)) {
                List<Train> trains = trainDAO.getAllTrains();
                resp.getWriter().write(gson.toJson(trains));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"Admin endpoint not found\"}");
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void handleAdminLogin(JsonObject body, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (body == null || !body.has("email") || !body.has("password")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Email and password required.\"}");
            return;
        }

        String email = body.get("email").getAsString().trim();
        String password = body.get("password").getAsString();

        String configuredEmail = System.getenv("ADMIN_EMAIL");
        if (configuredEmail == null || configuredEmail.isEmpty()) configuredEmail = DEFAULT_ADMIN_EMAIL;

        String configuredPassword = System.getenv("ADMIN_PASSWORD");
        if (configuredPassword == null || configuredPassword.isEmpty()) configuredPassword = DEFAULT_ADMIN_PASSWORD;

        boolean isValid = false;

        // Check configured admin credentials
        if (configuredEmail.equalsIgnoreCase(email) && configuredPassword.equals(password)) {
            isValid = true;
        } else {
            // Check database admin
            try {
                User dbUser = userDAO.findByEmailOrPhone(email);
                if (dbUser != null && "ADMIN".equalsIgnoreCase(dbUser.getRole()) && PasswordUtil.checkPassword(password, dbUser.getPasswordHash())) {
                    isValid = true;
                }
            } catch (SQLException ignored) {}
        }

        if (isValid) {
            HttpSession session = req.getSession(true);
            session.setAttribute("userId", 1);
            session.setAttribute("userName", "System Administrator");
            session.setAttribute("userEmail", email);
            session.setAttribute("userRole", "ADMIN");

            JsonObject res = new JsonObject();
            res.addProperty("success", true);
            res.addProperty("message", "Admin authentication successful!");
            res.addProperty("role", "ADMIN");
            res.addProperty("name", "System Administrator");
            resp.getWriter().write(gson.toJson(res));
        } else {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"error\": \"Invalid Administrator credentials.\"}");
        }
    }

    private void handleTicketDecision(JsonObject body, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (body == null || !body.has("bookingId") || !body.has("decision")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Booking ID and decision action required.\"}");
            return;
        }

        String bookingId = body.get("bookingId").getAsString().trim();
        String decision = body.get("decision").getAsString().trim().toUpperCase();

        // Must be one of the three decision states
        if (!"CONFIRMED".equals(decision) && !"WAITING LIST".equals(decision) && !"NOT SELECTED".equals(decision)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Decision must be CONFIRMED, WAITING LIST, or NOT SELECTED.\"}");
            return;
        }

        try {
            boolean updated = bookingDAO.updateBookingStatus(bookingId, decision);
            if (updated) {
                JsonObject res = new JsonObject();
                res.addProperty("success", true);
                res.addProperty("message", "Ticket status successfully updated to " + decision);
                res.addProperty("bookingId", bookingId);
                res.addProperty("newStatus", decision);
                resp.getWriter().write(gson.toJson(res));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"Booking not found for update.\"}");
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void handleAddTrain(JsonObject body, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Train train = new Train();
            train.setTrainNumber(body.get("trainNumber").getAsString());
            train.setTrainName(body.get("trainName").getAsString());
            train.setSource(body.get("source").getAsString());
            train.setDestination(body.get("destination").getAsString());
            train.setDepartureTime(body.get("departureTime").getAsString());
            train.setArrivalTime(body.get("arrivalTime").getAsString());
            train.setTotalSeats(body.get("totalSeats").getAsInt());
            train.setAvailableSeats(body.get("availableSeats").getAsInt());
            train.setAcSeats(body.get("acSeats").getAsInt());
            train.setSleeperSeats(body.get("sleeperSeats").getAsInt());
            train.setTatkalSeats(body.get("tatkalSeats").getAsInt());
            train.setFareAc(new BigDecimal(body.get("fareAc").getAsString()));
            train.setFareSleeper(new BigDecimal(body.get("fareSleeper").getAsString()));
            train.setFareGeneral(new BigDecimal("180.00"));
            train.setTatkalSurcharge(new BigDecimal("150.00"));

            boolean added = trainDAO.addTrain(train);
            if (added) {
                resp.getWriter().write("{\"success\": true, \"message\": \"Train added successfully!\", \"trainId\": " + train.getTrainId() + "}");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\": \"Failed to add train.\"}");
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void handleUpdateTrain(JsonObject body, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Train train = new Train();
            train.setTrainId(body.get("trainId").getAsInt());
            train.setTrainName(body.get("trainName").getAsString());
            train.setSource(body.get("source").getAsString());
            train.setDestination(body.get("destination").getAsString());
            train.setDepartureTime(body.get("departureTime").getAsString());
            train.setArrivalTime(body.get("arrivalTime").getAsString());
            train.setTotalSeats(body.get("totalSeats").getAsInt());
            train.setAvailableSeats(body.get("availableSeats").getAsInt());
            train.setAcSeats(body.get("acSeats").getAsInt());
            train.setSleeperSeats(body.get("sleeperSeats").getAsInt());
            train.setTatkalSeats(body.get("tatkalSeats").getAsInt());
            train.setFareAc(new BigDecimal(body.get("fareAc").getAsString()));
            train.setFareSleeper(new BigDecimal(body.get("fareSleeper").getAsString()));

            boolean updated = trainDAO.updateTrain(train);
            resp.getWriter().write("{\"success\": " + updated + "}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void handleDeleteTrain(JsonObject body, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int trainId = body.get("trainId").getAsInt();
            boolean deleted = trainDAO.deleteTrain(trainId);
            resp.getWriter().write("{\"success\": " + deleted + "}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
