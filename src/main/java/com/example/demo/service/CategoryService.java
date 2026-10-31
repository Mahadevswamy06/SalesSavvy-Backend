package com.example.demo.service;

import java.util.List;
import com.example.demo.entity.Category;

public interface CategoryService {

    List<Category> getAllCategories();

    Category getCategoryById(Integer id);

    Category addCategory(Category category);

    Category updateCategory(Integer id, Category category);

    void deleteCategory(Integer id);
}