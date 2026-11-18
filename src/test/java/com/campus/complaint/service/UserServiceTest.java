package com.campus.complaint.service;

import com.campus.complaint.dto.AdminUserForm;
import com.campus.complaint.dto.RegisterRequest;
import com.campus.complaint.dto.UserResponse;
import com.campus.complaint.entity.Role;
import com.campus.complaint.entity.User;
import com.campus.complaint.exception.DuplicateResourceException;
import com.campus.complaint.exception.ResourceNotFoundException;
import com.campus.complaint.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SessionRegistry sessionRegistry;

    @InjectMocks
    private UserService userService;

    private RegisterRequest sampleRequest;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleRequest = new RegisterRequest();
        sampleRequest.setUsername("johndoe");
        sampleRequest.setPassword("secret123");
        sampleRequest.setEmail("john@campus.edu");
        sampleRequest.setFullName("John Doe");
        sampleRequest.setDepartment("Computer Science");
        sampleRequest.setPhone("1234567890");

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("johndoe");
        sampleUser.setPassword("encodedSecret");
        sampleUser.setEmail("john@campus.edu");
        sampleUser.setFullName("John Doe");
        sampleUser.setRole(Role.STUDENT);
        sampleUser.setEnabled(true);
    }

    @Test
    @DisplayName("Successfully register a new student user")
    void testRegisterUser_Success() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@campus.edu")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encodedSecret");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = userService.registerUser(sampleRequest);

        assertNotNull(response);
        assertEquals("johndoe", response.getUsername());
        assertEquals("john@campus.edu", response.getEmail());
        assertEquals(Role.STUDENT, response.getRole());
        verify(passwordEncoder).encode("secret123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Fail registration when username already exists")
    void testRegisterUser_DuplicateUsername() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.registerUser(sampleRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Fail registration when email already exists")
    void testRegisterUser_DuplicateEmail() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@campus.edu")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.registerUser(sampleRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Find user by ID successfully")
    void testGetUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserResponse response = userService.getUserById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("johndoe", response.getUsername());
    }

    @Test
    @DisplayName("Fail when user ID is not found")
    void testGetUserById_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    @DisplayName("Update user role")
    void testUpdateUserRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = userService.updateUserRole(1L, Role.MAINTENANCE_STAFF);

        assertNotNull(response);
        assertEquals(Role.MAINTENANCE_STAFF, sampleUser.getRole());
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Disabling a user expires their active sessions")
    void testToggleUserEnabled_ExpiresActiveSessions() {
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("johndoe").password("encodedSecret").roles("STUDENT").build();
        SessionInformation session = new SessionInformation(principal, "sess-1", new Date());

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(principal));
        when(sessionRegistry.getAllSessions(principal, false)).thenReturn(List.of(session));

        userService.toggleUserEnabled(1L);

        assertFalse(sampleUser.isEnabled());
        assertTrue(session.isExpired());
    }

    @Test
    @DisplayName("Create managed user successfully as maintenance staff")
    void testCreateManagedUser_Success() {
        AdminUserForm form = new AdminUserForm();
        form.setUsername("staff2");
        form.setEmail("staff2@campus.edu");
        form.setFullName("Staff Two");
        form.setPassword("password123");
        form.setDepartment("Electrical");
        form.setPhone("9876543210");

        when(userRepository.existsByUsername("staff2")).thenReturn(false);
        when(userRepository.existsByEmail("staff2@campus.edu")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedStaffPassword");

        User savedStaff = new User();
        savedStaff.setId(2L);
        savedStaff.setUsername("staff2");
        savedStaff.setEmail("staff2@campus.edu");
        savedStaff.setFullName("Staff Two");
        savedStaff.setRole(Role.MAINTENANCE_STAFF);
        savedStaff.setDepartment("Electrical");
        savedStaff.setEnabled(true);

        when(userRepository.save(any(User.class))).thenReturn(savedStaff);

        UserResponse response = userService.createManagedUser(form, Role.MAINTENANCE_STAFF);

        assertNotNull(response);
        assertEquals("staff2", response.getUsername());
        assertEquals(Role.MAINTENANCE_STAFF, response.getRole());
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Create managed user fails for invalid role ADMIN")
    void testCreateManagedUser_DisallowedRole() {
        AdminUserForm form = new AdminUserForm();
        form.setUsername("admin2");
        form.setPassword("password123");

        assertThrows(IllegalArgumentException.class, () ->
                userService.createManagedUser(form, Role.ADMIN));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Update managed user successfully")
    void testUpdateManagedUser_Success() {
        AdminUserForm form = new AdminUserForm();
        form.setUsername("johndoe_updated");
        form.setEmail("john_updated@campus.edu");
        form.setFullName("John Doe Updated");
        form.setPassword("newsecret123");
        form.setDepartment("Information Technology");
        form.setPhone("5555555555");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.findByUsername("johndoe_updated")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("john_updated@campus.edu")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("newsecret123")).thenReturn("encodedNewSecret");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = userService.updateManagedUser(1L, form);

        assertNotNull(response);
        assertEquals("johndoe_updated", sampleUser.getUsername());
        assertEquals("john_updated@campus.edu", sampleUser.getEmail());
        verify(passwordEncoder).encode("newsecret123");
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Update managed user rejects editing ADMIN accounts")
    void testUpdateManagedUser_DisallowedForAdmin() {
        User admin = new User("admin", "pass", "admin@campus.edu", "Admin", Role.ADMIN);
        admin.setId(10L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(admin));

        AdminUserForm form = new AdminUserForm();
        assertThrows(IllegalArgumentException.class, () ->
                userService.updateManagedUser(10L, form));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deactivate managed user successfully and expires active sessions")
    void testDeactivateManagedUser_Success_ExpiresSessions() {
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("johndoe").password("encodedSecret").roles("STUDENT").build();
        SessionInformation session = new SessionInformation(principal, "sess-2", new Date());

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(principal));
        when(sessionRegistry.getAllSessions(principal, false)).thenReturn(List.of(session));

        userService.deactivateManagedUser(1L, "admin");

        assertFalse(sampleUser.isEnabled());
        assertTrue(session.isExpired());
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Deactivate managed user rejects self-deactivation")
    void testDeactivateManagedUser_CannotDeactivateSelf() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        assertThrows(IllegalArgumentException.class, () ->
                userService.deactivateManagedUser(1L, "johndoe"));
        verify(userRepository, never()).save(any(User.class));
    }
}
