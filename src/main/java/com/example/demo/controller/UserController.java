package com.example.demo.controller;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "${FRONTEND_ORIGIN:http://localhost:5174}", allowCredentials = "true")
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            user.setRole(Role.CUSTOMER); // Public registration cannot create an administrator.
            User registeredUser = userService.registerUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "User registered successfully",
                    "user", safeUser(registeredUser)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        return ResponseEntity.ok(profileUser(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        try {
            Object usernameValue = body.get("username");
            Object nameValue = body.get("name");
            String requestedUsername = usernameValue != null ? usernameValue.toString().trim()
                    : (nameValue != null ? nameValue.toString().trim().replaceAll("\\s+", "_") : user.getUsername());
            if (requestedUsername.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Username cannot be blank"));
            userRepository.findByUsername(requestedUsername).filter(other -> !other.getUserId().equals(user.getUserId()))
                    .ifPresent(other -> { throw new IllegalArgumentException("Username is already taken"); });

            Object emailValue = body.get("email");
            String requestedEmail = emailValue == null ? user.getEmail() : emailValue.toString().trim();
            if (requestedEmail.isBlank() || !requestedEmail.contains("@")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Enter a valid email address"));
            }
            userRepository.findByEmail(requestedEmail).filter(other -> !other.getUserId().equals(user.getUserId()))
                    .ifPresent(other -> { throw new IllegalArgumentException("Email is already registered"); });
            user.setUsername(requestedUsername);
            user.setEmail(requestedEmail);
            user.setPhone(stringField(body, "phone", user.getPhone()));
            user.setAddress(stringField(body, "address", user.getAddress()));
            user.setCity(stringField(body, "city", user.getCity()));
            user.setState(stringField(body, "state", user.getState()));
            user.setPincode(stringField(body, "pincode", user.getPincode()));
            user.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(profileUser(userRepository.save(user)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        String currentPassword = body.getOrDefault("currentPassword", "");
        String newPassword = body.getOrDefault("newPassword", "");
        String confirmPassword = body.getOrDefault("confirmPassword", "");
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        if (!encoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));
        }
        if (newPassword.length() < 8) return ResponseEntity.badRequest().body(Map.of("error", "New password must contain at least 8 characters"));
        if (!newPassword.equals(confirmPassword)) return ResponseEntity.badRequest().body(Map.of("error", "New password and confirmation do not match"));
        user.setPassword(encoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    private String stringField(Map<String, Object> body, String key, String current) {
        if (!body.containsKey(key) || body.get(key) == null) return current;
        String value = body.get(key).toString().trim();
        return value.isEmpty() ? null : value;
    }

    private Map<String, Object> safeUser(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getUserId());
        result.put("userId", user.getUserId());
        result.put("username", user.getUsername());
        result.put("name", user.getUsername().replace('_', ' '));
        result.put("email", user.getEmail());
        result.put("phone", user.getPhone());
        result.put("address", user.getAddress());
        result.put("city", user.getCity());
        result.put("state", user.getState());
        result.put("pincode", user.getPincode());
        result.put("role", user.getRole().name());
        result.put("createdAt", user.getCreatedAt());
        return result;
    }

    private Map<String, Object> profileUser(User user) { return safeUser(user); }
}
