package com.campus.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.campus.complaint.entity.ComplaintStatus;

/**
 * DTO for updating complaint status.
 */
public class StatusUpdateRequest {

    @NotNull(message = "New status is required")
    private ComplaintStatus status;

    private String remarks;

    // Getters and Setters
    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
