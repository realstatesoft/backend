package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.dto.visit.CreateVisitRequestRequest;
import com.openroof.openroof.dto.visit.VisitRequestResponse;
import com.openroof.openroof.model.enums.VisitRequestStatus;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.service.VisitRequestService;
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

@WebMvcTest(VisitRequestController.class)
@Import({SecurityConfig.class, com.openroof.openroof.config.JacksonConfig.class, com.openroof.openroof.test.SliceSecurityBeans.class})
class VisitRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private VisitRequestService visitRequestService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

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

    private VisitRequestResponse sampleResponse(Long id) {
        LocalDateTime now = LocalDateTime.now();
        return new VisitRequestResponse(
                id,
                10L,
                "Casa Centro",
                20L,
                "Buyer",
                "buyer@openroof.com",
                "555-1111",
                30L,
                "Agent",
                now.plusDays(1),
                null,
                null,
                VisitRequestStatus.PENDING,
                "mensaje",
                null,
                now,
                now
        );
    }

    @Nested
    @DisplayName("POST /visit-requests")
    class CreateTests {

        @Test
        @DisplayName("USER puede crear solicitud y retorna 201")
        void createWithUser_returns201() throws Exception {
            CreateVisitRequestRequest request = new CreateVisitRequestRequest(
                    10L,
                    LocalDateTime.now().plusDays(1),
                    "Buyer",
                    "buyer@openroof.com",
                    "555-1111",
                    "Quiero visitar"
            );

            when(visitRequestService.create(any(CreateVisitRequestRequest.class), eq("user@test.com")))
                    .thenReturn(sampleResponse(100L));

            mockMvc.perform(post("/visit-requests")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(100L))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @DisplayName("AGENT no puede crear solicitud y retorna 403")
        void createWithAgent_returns403() throws Exception {
            CreateVisitRequestRequest request = new CreateVisitRequestRequest(
                    10L,
                    LocalDateTime.now().plusDays(1),
                    "Buyer",
                    "buyer@openroof.com",
                    "555-1111",
                    "Quiero visitar"
            );

            mockMvc.perform(post("/visit-requests")
                            .with(user("agent@test.com").roles("AGENT"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /visit-requests/{id}/accept")
    class AcceptTests {

        @Test
        @DisplayName("AGENT puede aceptar y retorna 200")
        void acceptWithAgent_returns200() throws Exception {
            when(visitRequestService.accept(100L, "agent@test.com"))
                    .thenReturn(sampleResponse(100L));

            mockMvc.perform(put("/visit-requests/100/accept")
                            .with(user("agent@test.com").roles("AGENT")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(100L));
        }
    }

    @Nested
    @DisplayName("GET /visit-requests/me/buyer")
    class QueryTests {

        @Test
        @DisplayName("USER lista sus solicitudes y retorna 200")
        void getMyRequestsAsBuyer_returns200() throws Exception {
            when(visitRequestService.getMyRequestsAsBuyer("user@test.com"))
                    .thenReturn(List.of(sampleResponse(1L), sampleResponse(2L)));

            mockMvc.perform(get("/visit-requests/me/buyer")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }
    }
}
