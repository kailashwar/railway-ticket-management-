package com.railbook.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.railbook.dao.UserDAO;
import com.railbook.model.User;
import com.railbook.util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;

public class AuthServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        if (pathInfo == null || "/".equals(pathInfo)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Missing action path\"}");
            return;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        JsonObject body = gson.fromJson(sb.toString(), JsonObject.class);

        if ("/register".equalsIgnoreCase(pathInfo)) {
            handleRegister(body, req, resp);
        } else if ("/login".equalsIgnoreCase(pathInfo)) {
            handleLogin(body, req, resp);
        } else if ("/logout".equalsIgnoreCase(pathInfo)) {
            handleLogout(req, resp);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\": \"Action not found\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        if ("/me".equalsIgnoreCase(pathInfo)) {
            HttpSession session = req.getSession(false);
            if (session != null && session.getAttribute("userId") != null) {
                JsonObject res = new JsonObject();
                res.addProperty("userId", (Integer) session.getAttribute("userId"));
                res.addProperty("name", (String) session.getAttribute("userName"));
                res.addProperty("email", (String) session.getAttribute("userEmail"));
                res.addProperty("role", (String) session.getAttribute("userRole"));
                resp.getWriter().write(gson.toJson(res));
            } else {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write("{\"error\": \"Not authenticated\"}");
            }
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\": \"Not found\"}");
        }
    }

    private void handleRegister(JsonObject body, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (body == null || !body.has("name") || !body.has("email") || !body.has("phone") || !body.has("password")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"All registration fields are required.\"}");
            return;
        }

        String name = body.get("name").getAsString().trim();
        String email = body.get("email").getAsString().trim();
        String phone = body.get("phone").getAsString().trim();
        String password = body.get("password").getAsString();

        if (password.length() < 6) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Password must be at least 6 characters long.\"}");
            return;
        }

        try {
            User existing = userDAO.findByEmailOrPhone(email);
            if (existing != null) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write("{\"error\": \"An account with this email already exists.\"}");
                return;
            }

            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPhone(phone);
            user.setPasswordHash(PasswordUtil.hashPassword(password));
            user.setRole("PASSENGER");

            boolean success = userDAO.registerUser(user);
            if (success) {
                HttpSession session = req.getSession(true);
                session.setAttribute("userId", user.getUserId());
                session.setAttribute("userName", user.getName());
                session.setAttribute("userEmail", user.getEmail());
                session.setAttribute("userRole", user.getRole());

                JsonObject res = new JsonObject();
                res.addProperty("success", true);
                res.addProperty("message", "Registration successful!");
                res.addProperty("userId", user.getUserId());
                res.addProperty("name", user.getName());
                resp.getWriter().write(gson.toJson(res));
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\": \"Registration failed. Please try again.\"}");
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void handleLogin(JsonObject body, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (body == null || !body.has("identifier") || !body.has("password")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Email/phone and password are required.\"}");
            return;
        }

        String identifier = body.get("identifier").getAsString().trim();
        String password = body.get("password").getAsString();

        try {
            User user = userDAO.findByEmailOrPhone(identifier);
            if (user == null || !PasswordUtil.checkPassword(password, user.getPasswordHash())) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write("{\"error\": \"Invalid credentials. Please check your email and password.\"}");
                return;
            }

            HttpSession session = req.getSession(true);
            session.setAttribute("userId", user.getUserId());
            session.setAttribute("userName", user.getName());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("userRole", user.getRole());

            JsonObject res = new JsonObject();
            res.addProperty("success", true);
            res.addProperty("message", "Login successful!");
            res.addProperty("userId", user.getUserId());
            res.addProperty("name", user.getName());
            res.addProperty("email", user.getEmail());
            res.addProperty("role", user.getRole());
            resp.getWriter().write(gson.toJson(res));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        resp.getWriter().write("{\"success\": true, \"message\": \"Logged out successfully\"}");
    }
}
