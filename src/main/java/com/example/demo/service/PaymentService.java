package com.example.demo.service;

import com.example.demo.entity.CartItem;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.OrderStatus;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {
    @Value("${razorpay.key_id}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret}")
    private String razorpayKeySecret;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;

    public PaymentService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                          CartRepository cartRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
    }

    /** Creates a Razorpay order and stores the pending order, address, payment choice and line items. */
    @Transactional
    public String createOrder(int userId, BigDecimal totalAmount, Map<String, String> shipping, String paymentChoice)
            throws RazorpayException {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order total must be greater than zero");
        }

        List<CartItem> cartItems = cartRepository.findCartItemsWithProductDetails(userId);
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Your cart is empty. Add products before checkout.");
        }
        validateShipping(shipping);

        RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", totalAmount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValueExact());
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "ss_" + System.currentTimeMillis());
        com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
        String razorpayOrderId = razorpayOrder.get("id");

        Order order = new Order();
        order.setOrderId(razorpayOrderId);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(normalizePaymentChoice(paymentChoice));
        order.setShippingName(shipping.get("fullName"));
        order.setShippingPhone(shipping.get("phone"));
        order.setShippingEmail(shipping.get("email"));
        order.setShippingAddress(shipping.get("address"));
        order.setShippingCity(shipping.get("city"));
        order.setShippingState(shipping.get("state"));
        order.setShippingPincode(shipping.get("pincode"));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Snapshot the cart at checkout, so order details remain even after the cart is cleared.
        for (CartItem cartItem : cartItems) {
            BigDecimal price = cartItem.getProduct().getPrice();
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProduct().getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPricePerUnit(price);
            orderItem.setTotalPrice(price.multiply(BigDecimal.valueOf(cartItem.getQuantity())).setScale(2, RoundingMode.HALF_UP));
            orderItemRepository.save(orderItem);
        }
        return razorpayOrderId;
    }

    @Transactional
    public boolean verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature, int userId) {
        try {
            if (isBlank(razorpayOrderId) || isBlank(razorpayPaymentId) || isBlank(razorpaySignature)) return false;
            Order order = orderRepository.findById(razorpayOrderId).orElse(null);
            if (order == null || order.getUserId() != userId) return false;
            if (order.getStatus() == OrderStatus.SUCCESS) return true; // Idempotent retry.
            if (order.getStatus() != OrderStatus.PENDING) return false;

            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", razorpaySignature);
            if (!com.razorpay.Utils.verifyPaymentSignature(attributes, razorpayKeySecret)) return false;

            // Save the actual payment instrument returned by Razorpay, not only the UI preference.
            try {
                RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
                com.razorpay.Payment payment = client.payments.fetch(razorpayPaymentId);
                String actualMethod = payment.get("method");
                if (!isBlank(actualMethod)) order.setPaymentMethod("RAZORPAY_" + actualMethod.toUpperCase());
            } catch (Exception ignored) {
                // Signature verification is authoritative; keep the user's Razorpay preference if lookup fails.
            }

            order.setStatus(OrderStatus.SUCCESS);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            cartRepository.deleteAllCartItemsByUserId(userId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void validateShipping(Map<String, String> shipping) {
        String[] required = {"fullName", "phone", "email", "address", "city", "state", "pincode"};
        for (String field : required) {
            if (shipping.get(field) == null || shipping.get(field).trim().isEmpty()) {
                throw new IllegalArgumentException("Delivery field is required: " + field);
            }
        }
        if (!shipping.get("phone").matches("[0-9+() -]{10,16}")) {
            throw new IllegalArgumentException("Enter a valid phone number");
        }
        if (!shipping.get("pincode").matches("[0-9]{6}")) {
            throw new IllegalArgumentException("Enter a valid 6-digit PIN code");
        }
    }

    private String normalizePaymentChoice(String choice) {
        if (choice == null || choice.isBlank()) return "RAZORPAY";
        String value = choice.trim().toUpperCase();
        if (value.equals("UPI")) return "RAZORPAY_UPI";
        if (value.equals("CARD")) return "RAZORPAY_CARD";
        return "RAZORPAY";
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
