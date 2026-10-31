package com.example.demo.controller;

import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.entity.WishlistItem;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.WishlistRepository;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.entity.ProductImage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@CrossOrigin(origins = "${FRONTEND_ORIGIN:http://localhost:5174}", allowCredentials = "true")
@RequestMapping("/api/wishlist")
public class WishlistController {
    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    public WishlistController(WishlistRepository wishlistRepository, ProductRepository productRepository,
                              ProductImageRepository productImageRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
    }

    @GetMapping
    public ResponseEntity<?> getWishlist(HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        List<Map<String, Object>> products = wishlistRepository.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId()).stream().map(this::toMap).toList();
        return ResponseEntity.ok(Map.of("products", products));
    }

    @PostMapping
    public ResponseEntity<?> addWishlistItem(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        try {
            Integer productId = Integer.valueOf(body.get("productId").toString());
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Product not found"));
            WishlistItem item = wishlistRepository.findByUser_UserIdAndProduct_ProductId(user.getUserId(), productId).orElseGet(() -> wishlistRepository.save(new WishlistItem(user, product)));
            return ResponseEntity.status(HttpStatus.CREATED).body(toMap(item));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "A valid productId is required")); }
    }

    @DeleteMapping("/{productId}")
    @Transactional
    public ResponseEntity<?> removeWishlistItem(@PathVariable Integer productId, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        wishlistRepository.deleteByUser_UserIdAndProduct_ProductId(user.getUserId(), productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<?> clearWishlist(HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        wishlistRepository.deleteByUser_UserId(user.getUserId());
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toMap(WishlistItem item) {
        Product p = item.getProduct(); Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", p.getProductId()); out.put("productId", p.getProductId()); out.put("name", p.getName());
        out.put("description", p.getDescription()); out.put("price", p.getPrice()); out.put("createdAt", item.getCreatedAt());
        List<ProductImage> images = productImageRepository.findByProduct_ProductId(p.getProductId());
        out.put("image", images.isEmpty() ? null : images.get(0).getImageUrl());
        return out;
    }
}
