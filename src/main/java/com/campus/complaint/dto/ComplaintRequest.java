package com.campus.complaint.dto;

import jakarta.validation.constraints.*;

/**
 * DTO for creating a new complaint.
 */
public class ComplaintRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Location is required")
    @Size(max = 200, message = "Location must be at most 200 characters")
    private String location;

    @NotNull(message = "Category is required")
    private Long categoryId;

    private String priority;

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
