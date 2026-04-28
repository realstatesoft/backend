package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.dto.lead.CreateLeadFromWizardRequest;
import com.openroof.openroof.dto.lead.LeadResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.PropertyCategory;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.security.LeadSecurity;
import com.openroof.openroof.service.LeadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class LeadControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LeadService leadService;

    @MockitoBean
    private LeadSecurity leadSecurity;

    private static final String API_BASE = "/api/leads";

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private LeadResponse sampleLeadResponse() {
        return new LeadResponse(
                1L, 10L, "Agent Test",
                null, // userId
                "John Doe", "john@example.com", "+595981234567",
                "sell_wizard", "Nuevo", "#3b82f6",
                "Solicitud desde Sell Wizard",
                Map.of("address", "Calle Test 123"),
                Collections.emptyList(),
                LocalDateTime.now()
        );
    }

    private CreateLeadFromWizardRequest sampleWizardRequest() {
        return new CreateLeadFromWizardRequest(
                10L,
                "John", "Doe",
                "+595981234567", "john@example.com",
                "Calle Test 123", -25.2867, -57.647,
                PropertyType.HOUSE, PropertyCategory.SALE,
                "150", "120", "2000",
                3, 1, 1, 2,
                false, 1, true, false, null,
                "GOOD", "GOOD", "GOOD", "GOOD", "granite",
                false, Collections.emptyList(),
                "FIRST_TIME", "2_3_months",
                null
        );
    }

    private User adminUser() {
        User u = User.builder()
                .email("admin@test.com")
                .passwordHash("hash")
                .name("Admin")
                .role(UserRole.ADMIN)
                .build();
        u.setId(99L);
        return u;
    }

    // ═══════════════════════════════════════════════════════════════
    // POST /wizard
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/leads/wizard")
    class CreateFromWizardTests {

        @Test
        @DisplayName("Crear lead desde wizard con datos válidos → 201")
        void createFromWizard_validRequest_returns201() throws Exception {
            when(leadService.createFromWizard(any(CreateLeadFromWizardRequest.class)))
                    .thenReturn(sampleLeadResponse());

            mockMvc.perform(post(API_BASE + "/wizard")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleWizardRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("John Doe"))
                    .andExpect(jsonPath("$.data.agentId").value(10))
                    .andExpect(jsonPath("$.data.status").value("Nuevo"));
        }

        @Test
        @DisplayName("Crear lead sin agentId → 400")
        void createFromWizard_missingAgentId_returns400() throws Exception {
            String invalidJson = """
                    {
                        "firstName": "John",
                        "lastName": "Doe",
                        "phone": "+595981234567",
                        "address": "Calle Test 123",
                        "propertyType": "HOUSE",
                        "category": "SALE"
                    }
                    """;

            mockMvc.perform(post(API_BASE + "/wizard")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Crear lead con agente no existente → 404")
        void createFromWizard_agentNotFound_returns404() throws Exception {
            when(leadService.createFromWizard(any(CreateLeadFromWizardRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Agente no encontrado con ID: 999"));

            CreateLeadFromWizardRequest request = new CreateLeadFromWizardRequest(
                    999L,
                    "John", "Doe",
                    "+595981234567", "john@example.com",
                    "Calle Test 123", null, null,
                    PropertyType.HOUSE, PropertyCategory.SALE,
                    null, null, null,
                    null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null, null,
                    null, null,
                    null, null,
                    null
            );

            mockMvc.perform(post(API_BASE + "/wizard")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Crear lead con categoría inválida → 400")
        void createFromWizard_invalidCategory_returns400() throws Exception {
            String invalidJson = """
                    {
                        "agentId": 10,
                        "firstName": "John",
                        "lastName": "Doe",
                        "phone": "+595981234567",
                        "address": "Calle Test 123",
                        "propertyType": "HOUSE",
                        "category": "INVALID_CATEGORY"
                    }
                    """;

            mockMvc.perform(post(API_BASE + "/wizard")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET /agent/{agentId}
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/leads/agent/{agentId}")
    class GetLeadsByAgentTests {

        @Test
        @DisplayName("Obtener leads sin autenticación → 403")
        void getLeadsByAgent_unauthenticated_returns403() throws Exception {
            mockMvc.perform(get(API_BASE + "/agent/10"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Obtener leads como ADMIN → 200")
        void getLeadsByAgent_asAdmin_returns200() throws Exception {
            Page<LeadResponse> page = new PageImpl<>(List.of(sampleLeadResponse()),
                    PageRequest.of(0, 20), 1);
            when(leadService.getLeadsByAgent(eq(10L), any())).thenReturn(page);

            mockMvc.perform(get(API_BASE + "/agent/10")
                            .with(user(adminUser())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET /{id}
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/leads/{id}")
    class GetLeadByIdTests {

        @Test
        @DisplayName("Obtener lead sin autenticación → 403")
        void getById_unauthenticated_returns403() throws Exception {
            mockMvc.perform(get(API_BASE + "/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Obtener lead como ADMIN → 200")
        void getById_asAdmin_returns200() throws Exception {
            when(leadService.getById(eq(1L))).thenReturn(sampleLeadResponse());

            mockMvc.perform(get(API_BASE + "/1")
                            .with(user(adminUser())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1));
        }

        @Test
        @DisplayName("Obtener lead no encontrado → 404")
        void getById_notFound_returns404() throws Exception {
            when(leadService.getById(eq(999L)))
                    .thenThrow(new ResourceNotFoundException("Lead no encontrado con ID: 999"));

            mockMvc.perform(get(API_BASE + "/999")
                            .with(user(adminUser())))
                    .andExpect(status().isNotFound());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET /agent/{agentId}/count
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/leads/agent/{agentId}/count")
    class CountByAgentTests {

        @Test
        @DisplayName("Contar leads sin autenticación → 403")
        void countByAgent_unauthenticated_returns403() throws Exception {
            mockMvc.perform(get(API_BASE + "/agent/10/count"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Contar leads como ADMIN → 200")
        void countByAgent_asAdmin_returns200() throws Exception {
            when(leadService.countByAgent(eq(10L))).thenReturn(5L);

            mockMvc.perform(get(API_BASE + "/agent/10/count")
                            .with(user(adminUser())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(5));
        }
    }
}
