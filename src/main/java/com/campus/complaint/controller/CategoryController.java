package com.campus.complaint.controller;

import com.campus.complaint.dto.ApiResponse;
import com.campus.complaint.entity.Category;
import com.campus.complaint.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing complaint categories.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAllCategories() {
        List<Category> categories = categoryService.getActiveCategories();
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved", categories));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getAllCategoriesIncludingInactive() {
        List<Category> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success("All categories retrieved", categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Category retrieved", category));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> createCategory(@RequestParam String name,
                                                       @RequestParam(required = false) String description) {
        Category category = categoryService.createCategory(name, description);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created", category));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateCategory(@PathVariable Long id,
                                                       @RequestParam String name,
                                                       @RequestParam(required = false) String description) {
        Category category = categoryService.updateCategory(id, name, description);
        return ResponseEntity.ok(ApiResponse.success("Category updated", category));
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> toggleCategory(@PathVariable Long id) {
        categoryService.toggleCategoryActive(id);
        return ResponseEntity.ok(ApiResponse.success("Category status toggled"));
    }
}
