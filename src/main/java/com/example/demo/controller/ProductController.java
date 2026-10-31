package com.example.demo.controller;

import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import com.example.demo.service.ProductService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "${FRONTEND_ORIGIN:http://localhost:5174}", allowCredentials = "true")
@RequestMapping("/api/products")
public class ProductController {

    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(required = false) String category) {
        try {
            List<Product> products = productService.getProductsByCategory(category);
            List<Map<String, Object>> productList = new ArrayList<>();
            for (Product product : products) productList.add(toProductMap(product));
            return ResponseEntity.ok(Map.of("products", productList));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Integer id) {
        return productRepository.findById(id)
                .<ResponseEntity<?>>map(product -> ResponseEntity.ok(toProductMap(product)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Upload a color-specific image to the backend and store its URL in products.color_images.
     * POST /api/products/{id}/color-image?color=Black with multipart field name "file".
     */
    @PostMapping(value = "/{id}/color-image", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadColorImage(@PathVariable Integer id,
            @RequestParam String color, @RequestParam("file") MultipartFile file) {
        try {
            Product product = productRepository.findById(id).orElse(null);
            if (product == null) return ResponseEntity.notFound().build();
            if (color == null || color.isBlank() || file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Color and image file are required"));
            }
            String contentType = file.getContentType() == null ? "" : file.getContentType();
            if (!contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed"));
            }
            String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
            String extension = original.lastIndexOf('.') >= 0 ? original.substring(original.lastIndexOf('.')) : ".jpg";
            String filename = id + "-" + UUID.randomUUID() + extension.replaceAll("[^A-Za-z0-9.]", "");
            Path uploadDir = Paths.get("uploads", "products").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(filename).normalize();
            if (!target.startsWith(uploadDir)) return ResponseEntity.badRequest().body(Map.of("error", "Invalid filename"));
            file.transferTo(target);

            Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            if (product.getColorImages() != null && !product.getColorImages().isBlank()) {
                try { map.putAll(objectMapper.readValue(product.getColorImages(), new TypeReference<Map<String, String>>() {})); }
                catch (Exception ignored) { /* replace malformed legacy value with a valid mapping */ }
            }
            String imageUrl = "/uploads/products/" + filename;
            map.put(color.trim(), imageUrl);
            product.setColorImages(objectMapper.writeValueAsString(map));
            productRepository.save(product);
            return ResponseEntity.ok(Map.of("productId", id, "color", color.trim(), "imageUrl", imageUrl, "colorImages", map));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Image upload failed: " + e.getMessage()));
        }
    }

    private Map<String, Object> toProductMap(Product product) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("product_id", product.getProductId());
        data.put("id", product.getProductId());
        data.put("name", product.getName());
        data.put("description", product.getDescription());
        data.put("price", product.getPrice());
        data.put("stock", product.getStock());
        data.put("colors", product.getColors());
        data.put("sizes", product.getSizes());
        data.put("colorImages", product.getColorImages());
        Category category = product.getCategory();
        if (category != null) {
            data.put("category", Map.of(
                    "categoryId", category.getCategoryId(),
                    "categoryName", category.getCategoryName()));
            data.put("categoryId", category.getCategoryId());
            data.put("categoryName", category.getCategoryName());
        }
        List<String> images = productService.getProductImages(product.getProductId());
        if (images.isEmpty() && product.getColorImages() != null && !product.getColorImages().isBlank()) {
            try {
                Map<String, String> colorMap = objectMapper.readValue(product.getColorImages(), new TypeReference<Map<String, String>>() {});
                images.addAll(colorMap.values());
            } catch (Exception ignored) { /* malformed/legacy color_images is ignored */ }
        }
        data.put("images", images);
        if (!images.isEmpty()) data.put("imageUrl", images.get(0));
        return data;
    }
}
