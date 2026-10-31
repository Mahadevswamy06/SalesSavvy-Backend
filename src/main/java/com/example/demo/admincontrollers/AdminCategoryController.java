package com.example.demo.admincontrollers;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.adminservices.AdminCategoryService;
import com.example.demo.entity.Category;

@RestController
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    public AdminCategoryController(
            AdminCategoryService adminCategoryService) {
        this.adminCategoryService = adminCategoryService;
    }

    // 1. View all categories
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {

        return ResponseEntity.ok(
                adminCategoryService.getAllCategories());
    }

    // 2. View one category
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(
            @PathVariable Integer id) {

        try {
            return ResponseEntity.ok(
                    adminCategoryService.getCategoryById(id));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    // 3. Add category
    @PostMapping
    public ResponseEntity<?> addCategory(
            @RequestBody Category category) {

        try {
            Category savedCategory =
                    adminCategoryService.addCategory(category);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(savedCategory);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    // 4. Update category
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(
            @PathVariable Integer id,
            @RequestBody Category category) {

        try {
            Category updatedCategory =
                    adminCategoryService.updateCategory(id, category);

            return ResponseEntity.ok(updatedCategory);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    // 5. Delete category
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(
            @PathVariable Integer id) {

        try {
            adminCategoryService.deleteCategory(id);

            return ResponseEntity.ok(
                    "Category deleted successfully");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Cannot delete this category because products "
                            + "or other records reference it.");
        }
    }
}