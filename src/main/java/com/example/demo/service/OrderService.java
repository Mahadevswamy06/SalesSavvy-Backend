package com.example.demo.service;


import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
public class OrderService {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    /**
     * Fetches all successful orders for a given user and returns the required response format.
     *
     * @param user The authenticated user object.
     * @return A map containing the user's role, username, and ordered products.
     */
    public Map<String, Object> getOrdersForUser(User user) {
        // Fetch all successful order items for the user
        List<OrderItem> orderItems = orderItemRepository.findSuccessfulOrderItemsByUserId(user.getUserId());

        // Prepare the response map
        Map<String, Object> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("role", user.getRole()); // Directly use the role as it is an enum mapped to a string

        // Transform order items into a list of product details
        List<Map<String, Object>> products = new ArrayList<>();
        for (OrderItem item : orderItems) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product == null) {
                continue; // Skip if the product does not exist
            }

            // Fetch the product image (if available)
            List<ProductImage> images = productImageRepository.findByProduct_ProductId(product.getProductId());
            String imageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();

            // Create a product details map
            Map<String, Object> productDetails = new HashMap<>();
            productDetails.put("order_id", item.getOrder().getOrderId());
            productDetails.put("quantity", item.getQuantity());
            productDetails.put("total_price", item.getTotalPrice());
            productDetails.put("image_url", imageUrl);
            productDetails.put("product_id", product.getProductId());
            productDetails.put("name", product.getName());
            productDetails.put("description", product.getDescription());
            productDetails.put("price_per_unit", item.getPricePerUnit());
            productDetails.put("payment_method", item.getOrder().getPaymentMethod());
            productDetails.put("status", item.getOrder().getOrderStatus().name());
            productDetails.put("payment_status", item.getOrder().getStatus().name());
            productDetails.put("created_at", item.getOrder().getCreatedAt());
            productDetails.put("shipping_name", item.getOrder().getShippingName());
            productDetails.put("shipping_phone", item.getOrder().getShippingPhone());
            productDetails.put("shipping_email", item.getOrder().getShippingEmail());
            productDetails.put("shipping_address", item.getOrder().getShippingAddress());
            productDetails.put("shipping_city", item.getOrder().getShippingCity());
            productDetails.put("shipping_state", item.getOrder().getShippingState());
            productDetails.put("shipping_pincode", item.getOrder().getShippingPincode());

            products.add(productDetails);
        }

        // Add the products list to the response
        response.put("products", products);

        return response;
    }
    /** Returns orders for the admin dashboard without exposing customer account credentials. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllOrdersForAdmin() {
        List<Order> orders = orderRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> row = new HashMap<>();
            row.put("orderId", order.getOrderId());
            row.put("userId", order.getUserId());
            row.put("totalAmount", order.getTotalAmount());
            row.put("paymentStatus", order.getStatus().name());
            row.put("status", order.getOrderStatus().name());
            row.put("paymentMethod", order.getPaymentMethod());
            row.put("createdAt", order.getCreatedAt());
            row.put("shippingName", order.getShippingName());
            row.put("shippingPhone", order.getShippingPhone());
            row.put("shippingEmail", order.getShippingEmail());
            row.put("shippingAddress", order.getShippingAddress());
            row.put("shippingCity", order.getShippingCity());
            row.put("shippingState", order.getShippingState());
            row.put("shippingPincode", order.getShippingPincode());
            result.add(row);
        }
        return result;
    }

    /** Only the admin controller should expose this operation. */
    @Transactional
    public Map<String, Object> updateOrderStatus(String orderId, String requestedStatus) {
        if (requestedStatus == null || requestedStatus.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order status is required");
        }

        final OrderFulfillmentStatus newStatus;
        try {
            newStatus = OrderFulfillmentStatus.valueOf(requestedStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid order status. Allowed values: PROCESSING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        // Avoid changing fulfillment for an unpaid/failed order.
        if (order.getStatus() != OrderStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only successfully paid orders can have their fulfillment status updated");
        }

        order.setOrderStatus(newStatus);
        order.setUpdatedAt(java.time.LocalDateTime.now());
        Order saved = orderRepository.save(order);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Order status updated successfully");
        response.put("orderId", saved.getOrderId());
        response.put("status", saved.getOrderStatus().name());
        return response;
    }

}

