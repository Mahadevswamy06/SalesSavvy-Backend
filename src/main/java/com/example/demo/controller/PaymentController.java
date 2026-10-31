package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.razorpay.RazorpayException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "${FRONTEND_ORIGIN:http://localhost:5174}", allowCredentials = "true")
@RequestMapping("/api/payment")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createPaymentOrder(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        try {
            Object amountValue = body.get("totalAmount");
            if (amountValue == null) return ResponseEntity.badRequest().body(Map.of("error", "totalAmount is required"));
            BigDecimal totalAmount = new BigDecimal(amountValue.toString());

            Map<String, Object> rawShipping = body.get("shippingAddress") instanceof Map
                    ? (Map<String, Object>) body.get("shippingAddress") : body;
            Map<String, String> shipping = new LinkedHashMap<>();
            for (String field : new String[]{"fullName", "phone", "email", "address", "city", "state", "pincode"}) {
                Object value = rawShipping.get(field);
                shipping.put(field, value == null ? null : value.toString().trim());
            }
            String paymentChoice = body.get("paymentMethod") == null ? null : body.get("paymentMethod").toString();
            String razorpayOrderId = paymentService.createOrder(user.getUserId(), totalAmount, shipping, paymentChoice);
            return ResponseEntity.ok(razorpayOrderId);
        } catch (RazorpayException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "Razorpay could not create the order. Check backend Razorpay credentials."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Could not create payment order"));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        try {
            String orderId = stringValue(body.get("razorpayOrderId"));
            String paymentId = stringValue(body.get("razorpayPaymentId"));
            String signature = stringValue(body.get("razorpaySignature"));
            boolean verified = paymentService.verifyPayment(orderId, paymentId, signature, user.getUserId());
            if (verified) return ResponseEntity.ok(Map.of("message", "Payment verified and order saved", "orderId", orderId));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Payment verification failed or order does not belong to this user"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Could not verify payment"));
        }
    }

    private String stringValue(Object value) { return value == null ? null : value.toString(); }
}
