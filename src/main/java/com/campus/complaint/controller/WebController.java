package com.campus.complaint.controller;

import com.campus.complaint.dto.*;
import com.campus.complaint.entity.*;
import com.campus.complaint.service.*;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Web controller serving Thymeleaf pages for the browser-based UI.
 */
@Controller
public class WebController {

    private final UserService userService;
    private final ComplaintService complaintService;
    private final CategoryService categoryService;

    public WebController(UserService userService, ComplaintService complaintService,
                         CategoryService categoryService) {
        this.userService = userService;
        this.complaintService = complaintService;
        this.categoryService = categoryService;
    }

    // ==================== PUBLIC PAGES ====================

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                               BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "register";
        }
        try {
            userService.registerUser(request);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    // ==================== DASHBOARD ROUTER ====================

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth) {
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        } else if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MAINTENANCE_STAFF"))) {
            return "redirect:/staff/dashboard";
        }
        return "redirect:/student/dashboard";
    }

    // ==================== STUDENT PAGES ====================

    @GetMapping("/student/dashboard")
    public String studentDashboard(Authentication auth, Model model) {
        List<ComplaintResponse> complaints = complaintService.getComplaintsByUser(auth.getName());
        model.addAttribute("complaints", complaints);
        model.addAttribute("username", auth.getName());
        return "student/dashboard";
    }

    @GetMapping("/student/complaints/new")
    public String newComplaintForm(Model model) {
        model.addAttribute("complaintRequest", new ComplaintRequest());
        model.addAttribute("categories", categoryService.getActiveCategories());
        return "student/new-complaint";
    }

    @PostMapping("/student/complaints/new")
    public String submitComplaint(@Valid @ModelAttribute("complaintRequest") ComplaintRequest request,
                                  BindingResult result, Authentication auth, Model model,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getActiveCategories());
            return "student/new-complaint";
        }
        try {
            complaintService.createComplaint(request, auth.getName());
            redirectAttributes.addFlashAttribute("success", "Complaint submitted successfully!");
            return "redirect:/student/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.getActiveCategories());
            return "student/new-complaint";
        }
    }

    @GetMapping("/student/complaints/{id}")
    public String viewComplaint(@PathVariable Long id, Authentication auth, Model model) {
        ComplaintResponse complaint = complaintService.getComplaintById(id, auth.getName());
        List<ComplaintHistoryResponse> history = complaintService.getComplaintHistory(id, auth.getName());
        model.addAttribute("complaint", complaint);
        model.addAttribute("history", history);
        return "student/complaint-detail";
    }

    // ==================== ADMIN PAGES ====================

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        DashboardStats stats = complaintService.getDashboardStats();
        List<ComplaintResponse> complaints = complaintService.getAllComplaints();
        model.addAttribute("stats", stats);
        model.addAttribute("complaints", complaints);
        return "admin/dashboard";
    }

    @GetMapping("/admin/complaints/{id}")
    public String adminComplaintDetail(@PathVariable Long id, Authentication auth, Model model) {
        ComplaintResponse complaint = complaintService.getComplaintById(id, auth.getName());
        List<ComplaintHistoryResponse> history = complaintService.getComplaintHistory(id, auth.getName());
        List<UserResponse> staff = userService.getMaintenanceStaff();
        model.addAttribute("complaint", complaint);
        model.addAttribute("history", history);
        model.addAttribute("staff", staff);
        return "admin/complaint-detail";
    }

    @PostMapping("/admin/complaints/{id}/assign")
    public String assignComplaint(@PathVariable Long id, @RequestParam Long staffId,
                                  @RequestParam(required = false) String remarks,
                                  Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            AssignRequest request = new AssignRequest();
            request.setStaffId(staffId);
            request.setRemarks(remarks);
            complaintService.assignComplaint(id, request, auth.getName());
            redirectAttributes.addFlashAttribute("success", "Complaint assigned successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/complaints/" + id;
    }

    @PostMapping("/admin/complaints/{id}/status")
    public String adminUpdateStatus(@PathVariable Long id, @RequestParam ComplaintStatus status,
                                    @RequestParam(required = false) String remarks,
                                    Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            StatusUpdateRequest request = new StatusUpdateRequest();
            request.setStatus(status);
            request.setRemarks(remarks);
            complaintService.updateComplaintStatus(id, request, auth.getName());
            redirectAttributes.addFlashAttribute("success", "Status updated successfully!");
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/complaints/" + id;
    }

    @GetMapping("/admin/users")
    public String adminUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("userForm", new AdminUserForm());
        return "admin/users";
    }

    @PostMapping("/admin/users/new")
    public String adminCreateUser(@Valid @ModelAttribute("userForm") AdminUserForm form,
                                  BindingResult result, @RequestParam Role role,
                                  RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("error", "Please check the entered details. Username, name, email and password are required.");
            return "admin/users";
        }
        try {
            userService.createManagedUser(form, role);
            redirectAttributes.addFlashAttribute("success", role == Role.STUDENT
                    ? "Student account created successfully!" : "Maintenance staff account created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/users/{id}/edit")
    public String adminEditUser(@PathVariable Long id, Model model) {
        UserResponse user = userService.getUserById(id);
        if (user.getRole() != Role.STUDENT && user.getRole() != Role.MAINTENANCE_STAFF) {
            return "redirect:/admin/users";
        }
        AdminUserForm form = new AdminUserForm();
        form.setUsername(user.getUsername());
        form.setEmail(user.getEmail());
        form.setFullName(user.getFullName());
        form.setPhone(user.getPhone());
        form.setDepartment(user.getDepartment());
        model.addAttribute("user", user);
        model.addAttribute("userForm", form);
        return "admin/edit-user";
    }

    @PostMapping("/admin/users/{id}/edit")
    public String adminUpdateUser(@PathVariable Long id,
                                  @Valid @ModelAttribute("userForm") AdminUserForm form,
                                  BindingResult result, Model model,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("user", userService.getUserById(id));
            model.addAttribute("error", "Please check the entered details. Leave password blank to keep the current password.");
            return "admin/edit-user";
        }
        try {
            userService.updateManagedUser(id, form);
            redirectAttributes.addFlashAttribute("success", "User details updated successfully!");
            return "redirect:/admin/users";
        } catch (Exception e) {
            model.addAttribute("user", userService.getUserById(id));
            model.addAttribute("error", e.getMessage());
            return "admin/edit-user";
        }
    }

    @PostMapping("/admin/users/{id}/deactivate")
    public String adminDeactivateUser(@PathVariable Long id, Authentication auth,
                                      RedirectAttributes redirectAttributes) {
        try {
            userService.deactivateManagedUser(id, auth.getName());
            redirectAttributes.addFlashAttribute("success", "User account deactivated. Existing complaint history has been preserved.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/toggle-status")
    public String adminToggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            UserResponse user = userService.getUserById(id);
            if (user.getRole() != Role.STUDENT && user.getRole() != Role.MAINTENANCE_STAFF) {
                throw new IllegalArgumentException("Administrator accounts are protected");
            }
            userService.toggleUserEnabled(id);
            redirectAttributes.addFlashAttribute("success", "User account status updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/categories")
    public String adminCategories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/categories";
    }

    @PostMapping("/admin/categories/new")
    public String adminAddCategory(@RequestParam String name,
                                   @RequestParam(required = false) String description,
                                   RedirectAttributes redirectAttributes) {
        try {
            categoryService.createCategory(name, description);
            redirectAttributes.addFlashAttribute("success", "Category created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // ==================== STAFF PAGES ====================

    @GetMapping("/staff/dashboard")
    public String staffDashboard(Authentication auth, Model model) {
        List<ComplaintResponse> complaints = complaintService.getComplaintsByStaff(auth.getName());
        model.addAttribute("complaints", complaints);
        model.addAttribute("username", auth.getName());
        return "staff/dashboard";
    }

    @GetMapping("/staff/complaints/{id}")
    public String staffComplaintDetail(@PathVariable Long id, Authentication auth, Model model) {
        ComplaintResponse complaint = complaintService.getComplaintById(id, auth.getName());
        List<ComplaintHistoryResponse> history = complaintService.getComplaintHistory(id, auth.getName());
        model.addAttribute("complaint", complaint);
        model.addAttribute("history", history);
        return "staff/complaint-detail";
    }

    @PostMapping("/staff/complaints/{id}/status")
    public String staffUpdateStatus(@PathVariable Long id, @RequestParam ComplaintStatus status,
                                    @RequestParam(required = false) String remarks,
                                    Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            StatusUpdateRequest request = new StatusUpdateRequest();
            request.setStatus(status);
            request.setRemarks(remarks);
            complaintService.updateComplaintStatus(id, request, auth.getName());
            redirectAttributes.addFlashAttribute("success", "Status updated successfully!");
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/complaints/" + id;
    }
}
