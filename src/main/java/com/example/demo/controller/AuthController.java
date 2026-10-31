package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.LoginRequest;
import com.example.demo.entity.User;
import com.example.demo.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@CrossOrigin(origins = "${FRONTEND_ORIGIN:http://localhost:5174}", allowCredentials = "true")
@RequestMapping("/api/auth")

public class AuthController {
    private final AuthService authService;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${cookie.same-site:Lax}")
    private String cookieSameSite;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            User user = authService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
            String token = authService.generateToken(user);

            String secureAttribute = cookieSecure ? "; Secure" : "";
            response.addHeader("Set-Cookie",
                    String.format("authToken=%s; HttpOnly; Path=/; Max-Age=3600; SameSite=%s%s",
                            token, cookieSameSite, secureAttribute));

            
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "Login successful");
            responseBody.put("userId", user.getUserId());
            responseBody.put("username", user.getUsername());
            responseBody.put("role", user.getRole().name());

            return ResponseEntity.ok(responseBody);
            
        } 
        catch (RuntimeException e) 
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request,HttpServletResponse response) {
        try {
        	User user=(User) request.getAttribute("authenticatedUser");
            authService.logout(user);
            response.addHeader("Set-Cookie", "authToken=; HttpOnly; Path=/; Max-Age=0; SameSite="
                    + cookieSameSite + (cookieSecure ? "; Secure" : ""));
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("message", "Logout successful");
            return ResponseEntity.ok(responseBody);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Logout failed");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
}
