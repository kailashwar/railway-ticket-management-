package com.railbook.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            HttpServletResponse res = (HttpServletResponse) response;
            res.setHeader("X-Content-Type-Options", "nosniff");
            res.setHeader("X-Frame-Options", "SAMEORIGIN");
            res.setHeader("X-XSS-Protection", "1; mode=block");
            res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        }
        chain.doFilter(request, response);
    }
}
