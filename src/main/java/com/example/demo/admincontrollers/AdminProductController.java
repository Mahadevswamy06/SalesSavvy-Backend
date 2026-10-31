package com.example.demo.admincontrollers;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.adminservices.AdminProductService;

@RestController
@RequestMapping("/admin/products")
public class AdminProductController {

    @Autowired
    private AdminProductService adminProductService;

    // Add one product
    @PostMapping("/add")
    public ResponseEntity<?> addProduct(
            @RequestBody Map<String, Object> product) {

        try {
            String name = getRequiredString(product, "name");
            String description = getRequiredString(product, "description");

            double price = Double.parseDouble(
                    getRequiredString(product, "price"));

            int stock = Integer.parseInt(
                    getRequiredString(product, "stock"));

            int categoryId = Integer.parseInt(
                    getRequiredString(product, "categoryId"));

            String imageUrl = getRequiredString(product, "imageUrl");

            adminProductService.addProductWithImage(
                    name,
                    description,
                    price,
                    stock,
                    categoryId,
                    imageUrl
            );

            return ResponseEntity.ok("Product added successfully");

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body("Price, stock, and categoryId must be valid numbers");

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Failed to add product: " + e.getMessage());
        }
    }

    // Add multiple products
    @PostMapping("/add/bulk")
    public ResponseEntity<?> addMultipleProducts(
            @RequestBody List<Map<String, Object>> products) {

        try {
            if (products == null || products.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Product list cannot be empty");
            }

            adminProductService.addMultipleProducts(products);

            return ResponseEntity.ok(
                    products.size() + " products added successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Failed to add products: " + e.getMessage());
        }
    }

    // Delete a product
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteProduct(
            @RequestBody Map<String, Object> request) {

        try {
            if (request == null || request.get("productId") == null) {
                return ResponseEntity.badRequest()
                        .body("Product ID is required");
            }

            int productId = Integer.parseInt(
                    request.get("productId").toString());

            adminProductService.deleteProduct(productId);

            return ResponseEntity.ok("Product deleted successfully");

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body("Product ID must be a valid number");

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Failed to delete product: " + e.getMessage());
        }
    }

    // Validate required fields
    private String getRequiredString(
            Map<String, Object> product,
            String fieldName) {

        Object value = product.get(fieldName);

        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required");
        }

        return value.toString().trim();
    }
}
