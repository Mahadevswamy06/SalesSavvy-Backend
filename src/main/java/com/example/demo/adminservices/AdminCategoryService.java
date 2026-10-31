package com.example.demo.adminservices;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Category;
import com.example.demo.repository.CategoryRepository;

@Service
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;

    public AdminCategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // Get all categories
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // Get category by ID
    @Transactional(readOnly = true)
    public Category getCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Category not found with ID: " + id));
    }

    // Add category
    @Transactional
    public Category addCategory(Category category) {

        validateCategory(category);

        String name = category.getCategoryName().trim();

        if (categoryRepository.existsByCategoryName(name)) {
            throw new IllegalArgumentException(
                    "Category already exists: " + name);
        }

        category.setCategoryName(name);

        return categoryRepository.save(category);
    }

    // Update category
    @Transactional
    public Category updateCategory(Integer id, Category updatedCategory) {

        validateCategory(updatedCategory);

        Category existingCategory = getCategoryById(id);
        String name = updatedCategory.getCategoryName().trim();

        categoryRepository.findByCategoryName(name)
                .filter(category ->
                        !category.getCategoryId().equals(id))
                .ifPresent(category -> {
                    throw new IllegalArgumentException(
                            "Category name already exists: " + name);
                });

        existingCategory.setCategoryName(name);
        existingCategory.setCategoryImage(
                updatedCategory.getCategoryImage());

        return categoryRepository.save(existingCategory);
    }

    // Delete category
    @Transactional
    public void deleteCategory(Integer id) {

        Category category = getCategoryById(id);

        categoryRepository.delete(category);
    }

    // Validate category data
    private void validateCategory(Category category) {

        if (category == null) {
            throw new IllegalArgumentException(
                    "Category data is required");
        }

        if (category.getCategoryName() == null
                || category.getCategoryName().isBlank()) {
            throw new IllegalArgumentException(
                    "Category name is required");
        }

        if (category.getCategoryImage() != null
                && category.getCategoryImage().length() > 1000) {
            throw new IllegalArgumentException(
                    "Category image URL cannot exceed 1000 characters");
        }
    }
}