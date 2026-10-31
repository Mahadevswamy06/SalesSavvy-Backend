package com.example.demo.entity;

/** Delivery/fulfillment status, kept separate from payment status. */
public enum OrderFulfillmentStatus {
    PROCESSING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
