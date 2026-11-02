package com.railbook.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();
        HttpSession session = req.getSession(false);

        // Admin endpoints protection
        if (path.contains("/api/admin/")) {
            if (path.contains("/api/admin/login")) {
                chain.doFilter(request, response);
                return;
            }
            if (session == null || !"ADMIN".equals(session.getAttribute("userRole"))) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json");
                res.getWriter().write("{\"error\": \"Unauthorized. Admin privileges required.\"}");
                return;
            }
        }

        // TTR endpoints protection
        if (path.contains("/api/ttr/")) {
            if (path.contains("/api/ttr/login")) {
                chain.doFilter(request, response);
                return;
            }
            if (session == null || !"TTR".equals(session.getAttribute("userRole"))) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json");
                res.getWriter().write("{\"error\": \"Unauthorized. TTR credentials required.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
