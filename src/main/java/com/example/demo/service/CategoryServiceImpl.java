package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Category;
import com.example.demo.repository.CategoryRepository;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Category getCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Category not found with ID: " + id));
    }

    @Override
    public Category addCategory(Category category) {
        if (category.getCategoryName() == null
                || category.getCategoryName().isBlank()) {
            throw new IllegalArgumentException(
                    "Category name is required");
        }

        String name = category.getCategoryName().trim();

        if (categoryRepository.existsByCategoryName(name)) {
            throw new IllegalArgumentException(
                    "Category already exists: " + name);
        }

        category.setCategoryName(name);

        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(Integer id, Category updatedCategory) {
        Category existing = getCategoryById(id);

        if (updatedCategory.getCategoryName() == null
                || updatedCategory.getCategoryName().isBlank()) {
            throw new IllegalArgumentException(
                    "Category name is required");
        }

        String name = updatedCategory.getCategoryName().trim();

        categoryRepository.findByCategoryName(name)
                .filter(category -> !category.getCategoryId().equals(id))
                .ifPresent(category -> {
                    throw new IllegalArgumentException(
                            "Category name already exists: " + name);
                });

        existing.setCategoryName(name);
        existing.setCategoryImage(updatedCategory.getCategoryImage());

        return categoryRepository.save(existing);
    }

    @Override
    public void deleteCategory(Integer id) {
        Category category = getCategoryById(id);
        categoryRepository.delete(category);
    }
}
