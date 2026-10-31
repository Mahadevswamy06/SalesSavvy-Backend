package com.example.demo.admincontrollers;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "${FRONTEND_ORIGIN:http://localhost:5174}", allowCredentials = "true")
@RequestMapping({"/admin/orders", "/api/admin/orders"})
public class AdminOrderController {
    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllOrders(HttpServletRequest request) {
        requireAdmin(request);
        return ResponseEntity.ok(orderService.getAllOrdersForAdmin());
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        requireAdmin(request);
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, body.get("status")));
    }

    private void requireAdmin(HttpServletRequest request) {
        Object value = request.getAttribute("authenticatedUser");
        if (!(value instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in");
        }
        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
