
package com.example.demo.filter;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuthService;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebFilter(urlPatterns = {"/api/*", "/admin/*"})
public class AuthenticationFilter implements Filter {

    private static final Logger logger =
            LoggerFactory.getLogger(AuthenticationFilter.class);

    private final AuthService authService;
    private final UserRepository userRepository;

    private static final String ALLOWED_ORIGIN =
            System.getenv().getOrDefault(
                    "FRONTEND_ORIGIN",
                    "http://localhost:5174"
            );

    private static final String[] UNAUTHENTICATED_PATHS = {
            "/api/users/register",
            "/api/auth/login"
    };

    public AuthenticationFilter(
            AuthService authService,
            UserRepository userRepository) {

        this.authService = authService;
        this.userRepository = userRepository;

        logger.info("AuthenticationFilter initialized");
    }

    private boolean isPublicCatalogPath(String uri) {
        return uri.equals("/api/products")
                || uri.matches("/api/products/[0-9]+")
                || uri.equals("/api/categories")
                || uri.matches("/api/categories/[0-9]+");
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        executeFilterLogic(request, response, chain);
    }

    private void executeFilterLogic(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        logger.info("Request: {} {}", method, requestURI);

        // Handle CORS preflight requests.
        if ("OPTIONS".equalsIgnoreCase(method)) {
            setCORSHeaders(httpResponse);
            return;
        }

        // Public login, registration, and catalog browsing.
        if (Arrays.asList(UNAUTHENTICATED_PATHS).contains(requestURI)
                || isPublicCatalogPath(requestURI)) {

            chain.doFilter(request, response);
            return;
        }

        // Require a valid authentication token.
        String token = getAuthTokenFromCookies(httpRequest);

        if (token == null || !authService.validateToken(token)) {
            sendErrorResponse(
                    httpResponse,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: Invalid or missing token"
            );
            return;
        }

        // Find the user associated with the token.
        String username = authService.extractUsername(token);

        Optional<User> userOptional =
                userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            sendErrorResponse(
                    httpResponse,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: User not found"
            );
            return;
        }

        User authenticatedUser = userOptional.get();
        Role role = authenticatedUser.getRole();

        logger.info(
                "Authenticated User: {}, Role: {}",
                authenticatedUser.getUsername(),
                role
        );

        // Identify administrator-only endpoints.
        boolean adminApiPath =
                requestURI.startsWith("/admin/")
                || requestURI.startsWith("/api/admin/");

        boolean productImageUpload =
                requestURI.matches(
                        "/api/products/[0-9]+/color-image"
                );

        // Only administrators can use admin endpoints or upload
        // product color images.
        if ((adminApiPath || productImageUpload)
                && role != Role.ADMIN) {

            sendErrorResponse(
                    httpResponse,
                    HttpServletResponse.SC_FORBIDDEN,
                    "Forbidden: Admin access required"
            );
            return;
        }

        // Other protected API endpoints require a customer role.
        if (requestURI.startsWith("/api/")
                && !adminApiPath
                && !productImageUpload
                && role != Role.CUSTOMER) {

            sendErrorResponse(
                    httpResponse,
                    HttpServletResponse.SC_FORBIDDEN,
                    "Forbidden: Customer access required"
            );
            return;
        }

        // Make the authenticated user available to controllers.
        httpRequest.setAttribute(
                "authenticatedUser",
                authenticatedUser
        );

        chain.doFilter(request, response);
    }

    private void setCORSHeaders(HttpServletResponse response) {
        response.setHeader(
                "Access-Control-Allow-Origin",
                ALLOWED_ORIGIN
        );
        response.setHeader(
                "Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS"
        );
        response.setHeader(
                "Access-Control-Allow-Headers",
                "Content-Type, Authorization"
        );
        response.setHeader(
                "Access-Control-Allow-Credentials",
                "true"
        );

        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void sendErrorResponse(
            HttpServletResponse response,
            int statusCode,
            String message) throws IOException {

        response.setStatus(statusCode);
        response.setContentType("text/plain");
        response.getWriter().write(message);
    }

    private String getAuthTokenFromCookies(
            HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie ->
                            "authToken".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }
}
