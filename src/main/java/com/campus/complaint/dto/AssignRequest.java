package com.campus.complaint.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for assigning a complaint to maintenance staff.
 */
public class AssignRequest {

    @NotNull(message = "Staff ID is required")
    private Long staffId;

    private String remarks;

    // Getters and Setters
    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
