package com.campus.complaint.controller;

import com.campus.complaint.dto.*;
import com.campus.complaint.service.ComplaintService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for admin dashboard operations.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ComplaintService complaintService;

    public DashboardController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getDashboardStats() {
        DashboardStats stats = complaintService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Dashboard statistics retrieved", stats));
    }
}
