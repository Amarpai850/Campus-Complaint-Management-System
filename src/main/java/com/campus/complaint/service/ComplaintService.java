package com.campus.complaint.service;

import com.campus.complaint.dto.*;
import com.campus.complaint.entity.*;
import com.campus.complaint.exception.*;
import com.campus.complaint.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for complaint management — the core business logic.
 */
@Service
@Transactional
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    // Valid status transitions
    private static final Map<ComplaintStatus, Set<ComplaintStatus>> VALID_TRANSITIONS = Map.of(
        ComplaintStatus.PENDING,     Set.of(ComplaintStatus.ASSIGNED, ComplaintStatus.CLOSED),
        ComplaintStatus.ASSIGNED,    Set.of(ComplaintStatus.IN_PROGRESS, ComplaintStatus.PENDING, ComplaintStatus.CLOSED),
        ComplaintStatus.IN_PROGRESS, Set.of(ComplaintStatus.RESOLVED, ComplaintStatus.ASSIGNED, ComplaintStatus.CLOSED),
        ComplaintStatus.RESOLVED,    Set.of(ComplaintStatus.CLOSED, ComplaintStatus.IN_PROGRESS),
        ComplaintStatus.CLOSED,      Set.of()
    );

    public ComplaintService(ComplaintRepository complaintRepository,
                           ComplaintHistoryRepository historyRepository,
                           UserRepository userRepository,
                           CategoryRepository categoryRepository) {
        this.complaintRepository = complaintRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Create a new complaint.
     */
    public ComplaintResponse createComplaint(ComplaintRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        Complaint complaint = new Complaint();
        complaint.setTitle(request.getTitle());
        complaint.setDescription(request.getDescription());
        complaint.setLocation(request.getLocation());
        complaint.setUser(user);
        complaint.setCategory(category);
        complaint.setPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
        complaint.setStatus(ComplaintStatus.PENDING);

        Complaint saved = complaintRepository.save(complaint);

        // Record creation in history
        recordHistory(saved, null, ComplaintStatus.PENDING, "Complaint created", username);

        return toResponse(saved);
    }

    /**
     * Get a complaint by ID.
     */
    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(Long id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + id));
        return toResponse(complaint);
    }

    /**
     * Get all complaints (admin use).
     */
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getAllComplaints() {
        return complaintRepository.findAllOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get complaints by status.
     */
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaintsByStatus(ComplaintStatus status) {
        return complaintRepository.findByStatusOrderByCreatedAtDesc(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get complaints for a specific user.
     */
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaintsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return complaintRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get complaints assigned to a specific staff member.
     */
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaintsByStaff(String username) {
        User staff = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + username));
        return complaintRepository.findByAssignedStaffOrderByCreatedAtDesc(staff).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Assign a complaint to maintenance staff (admin operation).
     */
    public ComplaintResponse assignComplaint(Long complaintId, AssignRequest request, String adminUsername) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        User staff = userRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id: " + request.getStaffId()));

        if (staff.getRole() != Role.MAINTENANCE_STAFF) {
            throw new InvalidOperationException("User with id " + request.getStaffId() + " is not a maintenance staff member");
        }

        ComplaintStatus previousStatus = complaint.getStatus();
        complaint.setAssignedStaff(staff);
        complaint.setStatus(ComplaintStatus.ASSIGNED);

        Complaint saved = complaintRepository.save(complaint);

        String remarks = request.getRemarks() != null ? request.getRemarks()
                : "Assigned to " + staff.getFullName();
        recordHistory(saved, previousStatus, ComplaintStatus.ASSIGNED, remarks, adminUsername);

        return toResponse(saved);
    }

    /**
     * Update complaint status (used by maintenance staff or admin).
     */
    public ComplaintResponse updateComplaintStatus(Long complaintId, StatusUpdateRequest request, String username) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        ComplaintStatus currentStatus = complaint.getStatus();
        ComplaintStatus newStatus = request.getStatus();

        // Validate status transition
        Set<ComplaintStatus> allowedTransitions = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowedTransitions.contains(newStatus)) {
            throw new InvalidOperationException(
                    "Cannot transition from " + currentStatus + " to " + newStatus +
                    ". Allowed transitions: " + allowedTransitions);
        }

        complaint.setStatus(newStatus);
        if (newStatus == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(LocalDateTime.now());
        }

        Complaint saved = complaintRepository.save(complaint);

        String remarks = request.getRemarks() != null ? request.getRemarks()
                : "Status changed from " + currentStatus + " to " + newStatus;
        recordHistory(saved, currentStatus, newStatus, remarks, username);

        return toResponse(saved);
    }

    /**
     * Get complaint history.
     */
    @Transactional(readOnly = true)
    public List<ComplaintHistoryResponse> getComplaintHistory(Long complaintId) {
        // Verify complaint exists
        if (!complaintRepository.existsById(complaintId)) {
            throw new ResourceNotFoundException("Complaint not found with id: " + complaintId);
        }
        return historyRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId).stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get dashboard statistics.
     */
    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();
        stats.setTotalComplaints(complaintRepository.count());
        stats.setPendingComplaints(complaintRepository.countByStatus(ComplaintStatus.PENDING));
        stats.setAssignedComplaints(complaintRepository.countByStatus(ComplaintStatus.ASSIGNED));
        stats.setInProgressComplaints(complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS));
        stats.setResolvedComplaints(complaintRepository.countByStatus(ComplaintStatus.RESOLVED));
        stats.setClosedComplaints(complaintRepository.countByStatus(ComplaintStatus.CLOSED));
        stats.setTotalUsers(userRepository.count());
        stats.setTotalStudents(userRepository.countByRole(Role.STUDENT));
        stats.setTotalStaff(userRepository.countByRole(Role.MAINTENANCE_STAFF));
        stats.setTotalAdmins(userRepository.countByRole(Role.ADMIN));

        // Complaints by category
        Map<String, Long> byCategory = complaintRepository.countByCategory().stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? (String) row[0] : "Uncategorized",
                        row -> (Long) row[1]
                ));
        stats.setComplaintsByCategory(byCategory);

        return stats;
    }

    // ---- Private helpers ----

    private void recordHistory(Complaint complaint, ComplaintStatus previousStatus,
                               ComplaintStatus newStatus, String remarks, String updatedBy) {
        ComplaintHistory history = new ComplaintHistory(complaint, previousStatus, newStatus, remarks, updatedBy);
        historyRepository.save(history);
    }

    private ComplaintResponse toResponse(Complaint complaint) {
        ComplaintResponse response = new ComplaintResponse();
        response.setId(complaint.getId());
        response.setTitle(complaint.getTitle());
        response.setDescription(complaint.getDescription());
        response.setLocation(complaint.getLocation());
        response.setStatus(complaint.getStatus());
        response.setPriority(complaint.getPriority());
        response.setCreatedAt(complaint.getCreatedAt());
        response.setUpdatedAt(complaint.getUpdatedAt());
        response.setResolvedAt(complaint.getResolvedAt());

        if (complaint.getCategory() != null) {
            response.setCategoryName(complaint.getCategory().getName());
            response.setCategoryId(complaint.getCategory().getId());
        }
        if (complaint.getUser() != null) {
            response.setUserName(complaint.getUser().getFullName());
            response.setUserId(complaint.getUser().getId());
        }
        if (complaint.getAssignedStaff() != null) {
            response.setAssignedStaffName(complaint.getAssignedStaff().getFullName());
            response.setAssignedStaffId(complaint.getAssignedStaff().getId());
        }

        return response;
    }

    private ComplaintHistoryResponse toHistoryResponse(ComplaintHistory history) {
        ComplaintHistoryResponse response = new ComplaintHistoryResponse();
        response.setId(history.getId());
        response.setComplaintId(history.getComplaint().getId());
        response.setComplaintTitle(history.getComplaint().getTitle());
        response.setPreviousStatus(history.getPreviousStatus());
        response.setNewStatus(history.getNewStatus());
        response.setRemarks(history.getRemarks());
        response.setUpdatedBy(history.getUpdatedBy());
        response.setCreatedAt(history.getCreatedAt());
        return response;
    }
}
