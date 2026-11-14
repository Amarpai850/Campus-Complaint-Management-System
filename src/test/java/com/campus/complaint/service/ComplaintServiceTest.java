package com.campus.complaint.service;

import com.campus.complaint.dto.*;
import com.campus.complaint.entity.*;
import com.campus.complaint.exception.InvalidOperationException;
import com.campus.complaint.exception.ResourceNotFoundException;
import com.campus.complaint.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private ComplaintHistoryRepository historyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ComplaintService complaintService;

    private User student;
    private User staff;
    private Category category;
    private Complaint complaint;

    @BeforeEach
    void setUp() {
        student = new User("alice", "pass", "alice@campus.edu", "Alice Student", Role.STUDENT);
        student.setId(10L);

        staff = new User("bob", "pass", "bob@campus.edu", "Bob Plumber", Role.MAINTENANCE_STAFF);
        staff.setId(20L);

        category = new Category("Plumbing", "Water & pipes");
        category.setId(1L);

        complaint = new Complaint("Water Leak", "Water pipe leaking in Room 204", "Block B 204", student, category);
        complaint.setId(100L);
        complaint.setStatus(ComplaintStatus.PENDING);
        complaint.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Create complaint creates entity with PENDING status and records history")
    void testCreateComplaint_Success() {
        ComplaintRequest request = new ComplaintRequest();
        request.setTitle("Water Leak");
        request.setDescription("Water pipe leaking in Room 204");
        request.setLocation("Block B 204");
        request.setCategoryId(1L);
        request.setPriority("HIGH");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(student));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        ComplaintResponse response = complaintService.createComplaint(request, "alice");

        assertNotNull(response);
        assertEquals("Water Leak", response.getTitle());
        assertEquals(ComplaintStatus.PENDING, response.getStatus());
        verify(complaintRepository).save(any(Complaint.class));
        verify(historyRepository).save(any(ComplaintHistory.class));
    }

    @Test
    @DisplayName("Assign complaint to maintenance staff transitions status to ASSIGNED")
    void testAssignComplaint_Success() {
        AssignRequest request = new AssignRequest();
        request.setStaffId(20L);
        request.setRemarks("Assigned to Bob for plumbing fix");

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(complaint));
        when(userRepository.findById(20L)).thenReturn(Optional.of(staff));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        ComplaintResponse response = complaintService.assignComplaint(100L, request, "admin");

        assertNotNull(response);
        assertEquals(ComplaintStatus.ASSIGNED, complaint.getStatus());
        assertEquals(staff, complaint.getAssignedStaff());
        verify(historyRepository).save(any(ComplaintHistory.class));
    }

    @Test
    @DisplayName("Assign complaint fails if target user is not MAINTENANCE_STAFF")
    void testAssignComplaint_InvalidRole() {
        User nonStaff = new User("charlie", "pass", "charlie@campus.edu", "Charlie Student", Role.STUDENT);
        nonStaff.setId(30L);

        AssignRequest request = new AssignRequest();
        request.setStaffId(30L);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(complaint));
        when(userRepository.findById(30L)).thenReturn(Optional.of(nonStaff));

        assertThrows(InvalidOperationException.class, () ->
                complaintService.assignComplaint(100L, request, "admin"));
    }

    @Test
    @DisplayName("Update status: Valid transition ASSIGNED -> IN_PROGRESS")
    void testUpdateStatus_ValidTransition() {
        complaint.setStatus(ComplaintStatus.ASSIGNED);

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(ComplaintStatus.IN_PROGRESS);
        request.setRemarks("Started investigation");

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        ComplaintResponse response = complaintService.updateComplaintStatus(100L, request, "bob");

        assertNotNull(response);
        assertEquals(ComplaintStatus.IN_PROGRESS, complaint.getStatus());
        verify(historyRepository).save(any(ComplaintHistory.class));
    }

    @Test
    @DisplayName("Update status: Invalid transition PENDING -> RESOLVED rejected")
    void testUpdateStatus_InvalidTransition() {
        complaint.setStatus(ComplaintStatus.PENDING);

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(ComplaintStatus.RESOLVED);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(complaint));

        assertThrows(InvalidOperationException.class, () ->
                complaintService.updateComplaintStatus(100L, request, "bob"));
    }

    @Test
    @DisplayName("Get dashboard statistics aggregates counts properly")
    void testGetDashboardStats() {
        when(complaintRepository.count()).thenReturn(10L);
        when(complaintRepository.countByStatus(ComplaintStatus.PENDING)).thenReturn(3L);
        when(complaintRepository.countByStatus(ComplaintStatus.ASSIGNED)).thenReturn(2L);
        when(complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS)).thenReturn(2L);
        when(complaintRepository.countByStatus(ComplaintStatus.RESOLVED)).thenReturn(2L);
        when(complaintRepository.countByStatus(ComplaintStatus.CLOSED)).thenReturn(1L);
        when(userRepository.count()).thenReturn(15L);
        when(userRepository.countByRole(Role.STUDENT)).thenReturn(10L);
        when(userRepository.countByRole(Role.MAINTENANCE_STAFF)).thenReturn(3L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(2L);
        when(complaintRepository.countByCategory()).thenReturn(List.<Object[]>of(new Object[]{"Plumbing", 5L}));

        DashboardStats stats = complaintService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(10L, stats.getTotalComplaints());
        assertEquals(3L, stats.getPendingComplaints());
        assertEquals(15L, stats.getTotalUsers());
        assertEquals(1, stats.getComplaintsByCategory().size());
    }
}
