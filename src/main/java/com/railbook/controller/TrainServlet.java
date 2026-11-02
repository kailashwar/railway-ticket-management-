package com.railbook.controller;

import com.google.gson.Gson;
import com.railbook.dao.TrainDAO;
import com.railbook.model.Station;
import com.railbook.model.Train;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class TrainServlet extends HttpServlet {
    private final TrainDAO trainDAO = new TrainDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || "/".equals(pathInfo) || "/search".equalsIgnoreCase(pathInfo)) {
                String source = req.getParameter("from");
                String destination = req.getParameter("to");
                String travelClass = req.getParameter("class");
                String quota = req.getParameter("quota");
                boolean onlyAvailable = "true".equalsIgnoreCase(req.getParameter("available"));

                List<Train> trains = trainDAO.searchTrains(source, destination, travelClass, quota, onlyAvailable);
                resp.getWriter().write(gson.toJson(trains));
            } else if ("/stations".equalsIgnoreCase(pathInfo)) {
                List<Station> stations = trainDAO.getAllStations();
                resp.getWriter().write(gson.toJson(stations));
            } else {
                // Parse train ID from path /api/trains/1
                String idStr = pathInfo.substring(1);
                try {
                    int trainId = Integer.parseInt(idStr);
                    Train train = trainDAO.getTrainById(trainId);
                    if (train != null) {
                        resp.getWriter().write(gson.toJson(train));
                    } else {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().write("{\"error\": \"Train not found\"}");
                    }
                } catch (NumberFormatException e) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"error\": \"Invalid train ID\"}");
                }
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
