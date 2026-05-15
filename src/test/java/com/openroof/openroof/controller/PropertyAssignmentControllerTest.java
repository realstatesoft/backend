package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.config.SecurityHeadersFilter;
import com.openroof.openroof.dto.property.AssignPropertyRequest;
import com.openroof.openroof.dto.property.PropertyAssignmentResponse;
import com.openroof.openroof.model.enums.AssignmentStatus;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.PropertyViewRateLimitingFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.service.PropertyAssignmentService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PropertyAssignmentController.class)
@Import({SecurityConfig.class, com.openroof.openroof.config.JacksonConfig.class, com.openroof.openroof.test.SliceSecurityBeans.class})
class PropertyAssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PropertyAssignmentService assignmentService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private PropertyViewRateLimitingFilter propertyViewRateLimitingFilter;

    @MockitoBean
    private SecurityHeadersFilter securityHeadersFilter;

    @BeforeEach
    void setupJwtFilterPassThrough() throws Exception {
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthFilter).doFilter(
                any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    private PropertyAssignmentResponse sampleResponse(Long id, AssignmentStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return new PropertyAssignmentResponse(
                id,
                10L,
                "Casa Norte",
                20L,
                30L,
                "Agent",
                40L,
                "Owner",
                status,
                now,
                now
        );
    }

    @Nested
    @DisplayName("POST /properties/{propertyId}/assignments")
    class AssignTests {

        @Test
        @DisplayName("OWNER crea asignación y retorna 201")
        void assignWithOwner_returns201() throws Exception {
            AssignPropertyRequest request = new AssignPropertyRequest(20L);
            when(assignmentService.assign(eq(10L), any(AssignPropertyRequest.class), eq("owner@test.com")))
                    .thenReturn(sampleResponse(100L, AssignmentStatus.PENDING));

            mockMvc.perform(post("/properties/10/assignments")
                            .with(user("owner@test.com").roles("OWNER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(100L))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @DisplayName("USER sin rol OWNER/ADMIN recibe 403")
        void assignWithUser_returns403() throws Exception {
            AssignPropertyRequest request = new AssignPropertyRequest(20L);

            mockMvc.perform(post("/properties/10/assignments")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /assignments/{assignmentId}/accept")
    class AcceptTests {

        @Test
        @DisplayName("AGENT acepta asignación y retorna 200")
        void acceptWithAgent_returns200() throws Exception {
            when(assignmentService.accept(100L, "agent@test.com"))
                    .thenReturn(sampleResponse(100L, AssignmentStatus.ACCEPTED));

            mockMvc.perform(put("/assignments/100/accept")
                            .with(user("agent@test.com").roles("AGENT")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
        }
    }

    @Nested
    @DisplayName("GET /assignments/me")
    class QueryTests {

        @Test
        @DisplayName("AGENT obtiene sus asignaciones y retorna 200")
        void getMyAssignments_returns200() throws Exception {
            when(assignmentService.getMyAssignments("agent@test.com"))
                    .thenReturn(List.of(
                            sampleResponse(1L, AssignmentStatus.PENDING),
                            sampleResponse(2L, AssignmentStatus.ACCEPTED)
                    ));

            mockMvc.perform(get("/assignments/me")
                            .with(user("agent@test.com").roles("AGENT")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }
    }
}
