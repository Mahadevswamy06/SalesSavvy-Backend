package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@CrossOrigin(origins = "${FRONTEND_ORIGIN:http://localhost:5174}", allowCredentials = "true")
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) { this.orderService = orderService; }

    @GetMapping
    public ResponseEntity<?> getOrdersForUser(HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        try {
            return ResponseEntity.ok(orderService.getOrdersForUser(user));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Could not load order history"));
        }
    }
}
