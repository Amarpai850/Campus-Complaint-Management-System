package com.campus.complaint.service;

import com.campus.complaint.entity.Category;
import com.campus.complaint.exception.DuplicateResourceException;
import com.campus.complaint.exception.ResourceNotFoundException;
import com.campus.complaint.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing complaint categories.
 */
@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category createCategory(String name, String description) {
        if (categoryRepository.existsByName(name)) {
            throw new DuplicateResourceException("Category '" + name + "' already exists");
        }
        Category category = new Category(name, description);
        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Category> getActiveCategories() {
        return categoryRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    public Category updateCategory(Long id, String name, String description) {
        Category category = getCategoryById(id);
        if (!category.getName().equals(name) && categoryRepository.existsByName(name)) {
            throw new DuplicateResourceException("Category '" + name + "' already exists");
        }
        category.setName(name);
        category.setDescription(description);
        return categoryRepository.save(category);
    }

    public void toggleCategoryActive(Long id) {
        Category category = getCategoryById(id);
        category.setActive(!category.isActive());
        categoryRepository.save(category);
    }
}
