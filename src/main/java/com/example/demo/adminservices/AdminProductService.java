package com.example.demo.adminservices;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductImage;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.repository.ProductRepository;

@Service
public class AdminProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // Add one product
    @Transactional
    public void addProductWithImage(
            String name,
            String description,
            double price,
            int stock,
            int categoryId,
            String imageUrl) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }

        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }

        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }

        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("Image URL is required");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found: " + categoryId));

        Product product = new Product();

        product.setName(name);
        product.setDescription(description);
        product.setPrice(BigDecimal.valueOf(price));
        product.setStock(stock);
        product.setCategory(category);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);

        ProductImage productImage = new ProductImage();
        productImage.setProduct(savedProduct);
        productImage.setImageUrl(imageUrl);

        productImageRepository.save(productImage);
    }

    // Add multiple products in one request
    @Transactional
    public void addMultipleProducts(
            List<Map<String, Object>> products) {

        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException(
                    "Product list cannot be empty");
        }

        for (Map<String, Object> product : products) {

            if (product == null) {
                throw new IllegalArgumentException(
                        "Product data cannot be null");
            }

            String name = getRequiredString(product, "name");
            String description = getRequiredString(product, "description");

            double price;
            int stock;
            int categoryId;

            try {
                price = Double.parseDouble(
                        getRequiredString(product, "price"));

                stock = Integer.parseInt(
                        getRequiredString(product, "stock"));

                categoryId = Integer.parseInt(
                        getRequiredString(product, "categoryId"));

            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Price, stock, and categoryId must contain valid numbers");
            }

            String imageUrl = getRequiredString(product, "imageUrl");

            // Validate and save this product
            addProductWithImage(
                    name,
                    description,
                    price,
                    stock,
                    categoryId,
                    imageUrl
            );
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

    // Delete a product and its images
    @Transactional
    public void deleteProduct(Integer productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found: " + productId));

        productImageRepository.deleteByProductId(productId);
        productRepository.delete(product);
    }
}