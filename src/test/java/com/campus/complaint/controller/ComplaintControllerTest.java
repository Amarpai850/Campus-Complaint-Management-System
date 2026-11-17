package com.campus.complaint.controller;

import com.campus.complaint.dto.ComplaintRequest;
import com.campus.complaint.dto.ComplaintResponse;
import com.campus.complaint.entity.ComplaintStatus;
import com.campus.complaint.service.ComplaintService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:controllertest;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ComplaintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ComplaintService complaintService;

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    @DisplayName("Authenticated user can submit a complaint via REST API")
    void testCreateComplaint_Authenticated() throws Exception {
        ComplaintRequest req = new ComplaintRequest();
        req.setTitle("AC Not Working");
        req.setDescription("The AC in Lab 3 is blowing warm air.");
        req.setLocation("Computer Lab 3");
        req.setCategoryId(1L);
        req.setPriority("HIGH");

        ComplaintResponse mockResp = new ComplaintResponse();
        mockResp.setId(1L);
        mockResp.setTitle("AC Not Working");
        mockResp.setStatus(ComplaintStatus.PENDING);

        when(complaintService.createComplaint(any(ComplaintRequest.class), eq("student1")))
                .thenReturn(mockResp);

        mockMvc.perform(post("/api/complaints")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("AC Not Working"));
    }

    @Test
    @DisplayName("Unauthenticated request to submit complaint is rejected with 401")
    void testCreateComplaint_Unauthenticated() throws Exception {
        ComplaintRequest req = new ComplaintRequest();
        req.setTitle("AC Not Working");
        req.setDescription("The AC in Lab 3 is blowing warm air.");
        req.setLocation("Computer Lab 3");
        req.setCategoryId(1L);

        mockMvc.perform(post("/api/complaints")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    @DisplayName("Student can view their own complaints")
    void testGetMyComplaints() throws Exception {
        ComplaintResponse c = new ComplaintResponse();
        c.setId(10L);
        c.setTitle("Broken Chair");
        c.setStatus(ComplaintStatus.PENDING);

        when(complaintService.getComplaintsByUser("student1")).thenReturn(List.of(c));

        mockMvc.perform(get("/api/complaints/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].title").value("Broken Chair"));
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    @DisplayName("Student is forbidden from viewing all complaints endpoint (/api/complaints)")
    void testGetAllComplaints_ForbiddenForStudent() throws Exception {
        mockMvc.perform(get("/api/complaints"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin can view all complaints endpoint (/api/complaints)")
    void testGetAllComplaints_AllowedForAdmin() throws Exception {
        when(complaintService.getAllComplaints()).thenReturn(List.of());

        mockMvc.perform(get("/api/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "student2", roles = {"STUDENT"})
    @DisplayName("API returns 403 JSON when accessing another user's complaint")
    void testGetComplaintById_Forbidden() throws Exception {
        when(complaintService.getComplaintById(1L, "student2"))
                .thenThrow(new AccessDeniedException("You do not have permission to access this complaint"));

        mockMvc.perform(get("/api/complaints/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "student2", roles = {"STUDENT"})
    @DisplayName("Web view forwards to access-denied page when accessing another user's complaint")
    void testWebViewComplaint_Forbidden() throws Exception {
        when(complaintService.getComplaintById(1L, "student2"))
                .thenThrow(new AccessDeniedException("You do not have permission to access this complaint"));

        mockMvc.perform(get("/student/complaints/1"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"));
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    @DisplayName("Logout requires POST with CSRF token")
    void testLogout_RequiresPostWithCsrf() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }
}
