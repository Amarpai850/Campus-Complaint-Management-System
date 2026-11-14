package com.campus.complaint.service;

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
import org.springframework.security.crypto.password.PasswordEncoder;

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
}
