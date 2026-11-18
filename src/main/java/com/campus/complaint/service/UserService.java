package com.campus.complaint.service;

import com.campus.complaint.dto.AdminUserForm;
import com.campus.complaint.dto.RegisterRequest;
import com.campus.complaint.dto.UserResponse;
import com.campus.complaint.entity.Role;
import com.campus.complaint.entity.User;
import com.campus.complaint.exception.DuplicateResourceException;
import com.campus.complaint.exception.ResourceNotFoundException;
import com.campus.complaint.repository.UserRepository;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for user management operations.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessionRegistry;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, SessionRegistry sessionRegistry) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * Register a new user (default role: STUDENT).
     */
    public UserResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setDepartment(request.getDepartment());
        user.setRole(Role.STUDENT);

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    /**
     * Register a user with a specific role (admin use).
     */
    public UserResponse registerUserWithRole(RegisterRequest request, Role role) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setDepartment(request.getDepartment());
        user.setRole(role);

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public User getUserEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(Role role) {
        return userRepository.findByRole(role).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getMaintenanceStaff() {
        return getUsersByRole(Role.MAINTENANCE_STAFF);
    }

    public UserResponse updateUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setRole(newRole);
        User saved = userRepository.save(user);
        expireUserSessions(saved.getUsername());
        return toResponse(saved);
    }

    public void toggleUserEnabled(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setEnabled(!user.isEnabled());
        User saved = userRepository.save(user);
        expireUserSessions(saved.getUsername());
    }

    public UserResponse createManagedUser(AdminUserForm form, Role role) {
        if (role != Role.STUDENT && role != Role.MAINTENANCE_STAFF) {
            throw new IllegalArgumentException("Only student and maintenance staff accounts can be managed here");
        }
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new DuplicateResourceException("Username '" + form.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(form.getEmail())) {
            throw new DuplicateResourceException("Email '" + form.getEmail() + "' is already registered");
        }
        if (form.getPassword() == null || form.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must contain at least 6 characters");
        }
        User user = new User();
        user.setUsername(form.getUsername().trim());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setEmail(form.getEmail().trim());
        user.setFullName(form.getFullName().trim());
        user.setPhone(form.getPhone());
        user.setDepartment(form.getDepartment());
        user.setRole(role);
        return toResponse(userRepository.save(user));
    }

    public UserResponse updateManagedUser(Long id, AdminUserForm form) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        if (user.getRole() != Role.STUDENT && user.getRole() != Role.MAINTENANCE_STAFF) {
            throw new IllegalArgumentException("Administrator accounts cannot be edited here");
        }
        userRepository.findByUsername(form.getUsername()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) throw new DuplicateResourceException("Username is already taken");
        });
        userRepository.findByEmail(form.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) throw new DuplicateResourceException("Email is already registered");
        });
        user.setUsername(form.getUsername().trim());
        user.setEmail(form.getEmail().trim());
        user.setFullName(form.getFullName().trim());
        user.setPhone(form.getPhone());
        user.setDepartment(form.getDepartment());
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            if (form.getPassword().length() < 6) throw new IllegalArgumentException("Password must contain at least 6 characters");
            user.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        return toResponse(userRepository.save(user));
    }

    public void deactivateManagedUser(Long id, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        if (user.getRole() != Role.STUDENT && user.getRole() != Role.MAINTENANCE_STAFF) {
            throw new IllegalArgumentException("Administrator accounts cannot be deactivated here");
        }
        if (user.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("You cannot deactivate your own account");
        }
        user.setEnabled(false);
        User saved = userRepository.save(user);
        expireUserSessions(saved.getUsername());
    }

    private void expireUserSessions(String username) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            String principalName = principal instanceof UserDetails ud ? ud.getUsername() : principal.toString();
            if (username.equals(principalName)) {
                sessionRegistry.getAllSessions(principal, false)
                        .forEach(SessionInformation::expireNow);
            }
        }
    }

    /**
     * Map User entity to UserResponse DTO.
     */
    public UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setDepartment(user.getDepartment());
        response.setCreatedAt(user.getCreatedAt());
        response.setEnabled(user.isEnabled());
        return response;
    }
}
