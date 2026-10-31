package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@CrossOrigin(origins = "${FRONTEND_ORIGIN:http://localhost:5174}", allowCredentials = "true")
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) { this.cartService = cartService; }

    @GetMapping("/items/count")
    public ResponseEntity<?> getCartItemCount(HttpServletRequest request) {
        User user = authenticatedUser(request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        return ResponseEntity.ok(cartService.getCartItemCount(user.getUserId()));
    }

    @GetMapping("/items")
    public ResponseEntity<?> getCartItems(HttpServletRequest request) {
        User user = authenticatedUser(request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        return ResponseEntity.ok(cartService.getCartItems(user.getUserId()));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = authenticatedUser(request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        try {
            int productId = intValue(body.get("productId"), "productId");
            int quantity = body.containsKey("quantity") ? intValue(body.get("quantity"), "quantity") : 1;
            if (quantity < 1) return ResponseEntity.badRequest().body(Map.of("error", "Quantity must be at least 1"));
            cartService.addToCart(user.getUserId(), productId, quantity);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Item saved to cart"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateCartItemQuantity(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = authenticatedUser(request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        try {
            int productId = intValue(body.get("productId"), "productId");
            int quantity = intValue(body.get("quantity"), "quantity");
            if (quantity < 0) return ResponseEntity.badRequest().body(Map.of("error", "Quantity cannot be negative"));
            cartService.updateCartItemQuantity(user.getUserId(), productId, quantity);
            return ResponseEntity.ok(Map.of("message", quantity == 0 ? "Item removed from cart" : "Cart quantity updated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteCartItem(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = authenticatedUser(request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        try {
            int productId = intValue(body.get("productId"), "productId");
            cartService.deleteCartItem(user.getUserId(), productId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private User authenticatedUser(HttpServletRequest request) {
        return (User) request.getAttribute("authenticatedUser");
    }

    private int intValue(Object value, String field) {
        try { return Integer.parseInt(String.valueOf(value)); }
        catch (Exception e) { throw new IllegalArgumentException(field + " must be a valid integer"); }
    }
}
