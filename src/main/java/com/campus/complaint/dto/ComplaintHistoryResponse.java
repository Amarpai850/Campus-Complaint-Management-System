package com.campus.complaint.dto;

import com.campus.complaint.entity.ComplaintStatus;
import java.time.LocalDateTime;

/**
 * DTO for complaint history responses.
 */
public class ComplaintHistoryResponse {

    private Long id;
    private Long complaintId;
    private String complaintTitle;
    private ComplaintStatus previousStatus;
    private ComplaintStatus newStatus;
    private String remarks;
    private String updatedBy;
    private LocalDateTime createdAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }

    public String getComplaintTitle() { return complaintTitle; }
    public void setComplaintTitle(String complaintTitle) { this.complaintTitle = complaintTitle; }

    public ComplaintStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(ComplaintStatus previousStatus) { this.previousStatus = previousStatus; }

    public ComplaintStatus getNewStatus() { return newStatus; }
    public void setNewStatus(ComplaintStatus newStatus) { this.newStatus = newStatus; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
