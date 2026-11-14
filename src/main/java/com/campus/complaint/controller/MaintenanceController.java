package com.campus.complaint.controller;

import com.campus.complaint.dto.*;
import com.campus.complaint.service.ComplaintService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for maintenance staff operations.
 */
@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private final ComplaintService complaintService;

    public MaintenanceController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /**
     * Get complaints assigned to the currently logged-in maintenance staff.
     */
    @GetMapping("/my-assignments")
    @PreAuthorize("hasAnyRole('MAINTENANCE_STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse> getMyAssignments(Authentication auth) {
        List<ComplaintResponse> complaints = complaintService.getComplaintsByStaff(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Assigned complaints retrieved", complaints));
    }

    /**
     * Update complaint status (used by maintenance staff).
     */
    @PutMapping("/complaints/{id}/status")
    @PreAuthorize("hasAnyRole('MAINTENANCE_STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse> updateComplaintStatus(@PathVariable Long id,
                                                              @RequestBody StatusUpdateRequest request,
                                                              Authentication auth) {
        ComplaintResponse complaint = complaintService.updateComplaintStatus(id, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Complaint status updated", complaint));
    }
}
