package com.campus.complaint.controller;

import com.campus.complaint.dto.*;
import com.campus.complaint.entity.ComplaintStatus;
import com.campus.complaint.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for complaint operations.
 */
@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /**
     * Create a new complaint (any authenticated user).
     */
    @PostMapping
    public ResponseEntity<ApiResponse> createComplaint(@Valid @RequestBody ComplaintRequest request,
                                                       Authentication auth) {
        ComplaintResponse complaint = complaintService.createComplaint(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Complaint created successfully", complaint));
    }

    /**
     * Get all complaints (admin only).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getAllComplaints() {
        List<ComplaintResponse> complaints = complaintService.getAllComplaints();
        return ResponseEntity.ok(ApiResponse.success("Complaints retrieved", complaints));
    }

    /**
     * Get a specific complaint by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getComplaintById(@PathVariable Long id, Authentication auth) {
        ComplaintResponse complaint = complaintService.getComplaintById(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Complaint retrieved", complaint));
    }

    /**
     * Get complaints for the currently logged-in user.
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse> getMyComplaints(Authentication auth) {
        List<ComplaintResponse> complaints = complaintService.getComplaintsByUser(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Complaints retrieved", complaints));
    }

    /**
     * Get complaints by status (admin only).
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getComplaintsByStatus(@PathVariable ComplaintStatus status) {
        List<ComplaintResponse> complaints = complaintService.getComplaintsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success("Complaints retrieved", complaints));
    }

    /**
     * Assign a complaint to maintenance staff (admin only).
     */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> assignComplaint(@PathVariable Long id,
                                                       @Valid @RequestBody AssignRequest request,
                                                       Authentication auth) {
        ComplaintResponse complaint = complaintService.assignComplaint(id, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Complaint assigned successfully", complaint));
    }

    /**
     * Update complaint status (staff or admin).
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MAINTENANCE_STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse> updateStatus(@PathVariable Long id,
                                                    @Valid @RequestBody StatusUpdateRequest request,
                                                    Authentication auth) {
        ComplaintResponse complaint = complaintService.updateComplaintStatus(id, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Complaint status updated", complaint));
    }

    /**
     * Get complaint history.
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse> getComplaintHistory(@PathVariable Long id, Authentication auth) {
        List<ComplaintHistoryResponse> history = complaintService.getComplaintHistory(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Complaint history retrieved", history));
    }
}
