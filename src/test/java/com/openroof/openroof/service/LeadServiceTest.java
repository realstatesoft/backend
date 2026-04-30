package com.openroof.openroof.service;

import com.openroof.openroof.dto.lead.CreateLeadFromWizardRequest;
import com.openroof.openroof.dto.lead.LeadResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.PropertyCategory;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.lead.Lead;
import com.openroof.openroof.model.lead.LeadStatus;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.LeadInteractionRepository;
import com.openroof.openroof.repository.LeadRepository;
import com.openroof.openroof.repository.LeadStatusRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadStatusRepository leadStatusRepository;
    @Mock
    private LeadInteractionRepository leadInteractionRepository;

    @Mock
    private AgentProfileRepository agentProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LeadService leadService;

    private User testUser;
    private AgentProfile testAgent;
    private LeadStatus defaultStatus;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("agent@test.com")
                .passwordHash("hash")
                .name("Agent Test")
                .role(UserRole.AGENT)
                .build();
        testUser.setId(1L);

        testAgent = AgentProfile.builder()
                .user(testUser)
                .companyName("Test Realty")
                .avgRating(BigDecimal.ZERO)
                .totalReviews(0)
                .specialties(new ArrayList<>())
                .socialMedia(new ArrayList<>())
                .build();
        testAgent.setId(10L);

        defaultStatus = LeadStatus.builder()
                .name("Nuevo")
                .color("#3b82f6")
                .displayOrder(0)
                .active(true)
                .build();
        defaultStatus.setId(1L);

        lenient().when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(anyString()))
                .thenReturn(Optional.empty());
    }

    private CreateLeadFromWizardRequest sampleRequest() {
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

    private Lead buildSavedLead(CreateLeadFromWizardRequest request) {
        Lead lead = Lead.builder()
                .agent(testAgent)
                .status(defaultStatus)
                .name(request.getFullName())
                .email(request.email())
                .phone(request.phone())
                .source("sell_wizard")
                .notes("test notes")
                .metadata(null)
                .interactions(new ArrayList<>())
                .build();
        lead.setId(1L);
        lead.setCreatedAt(LocalDateTime.now());
        return lead;
    }

    // ═══════════════════════════════════════════════════════════════
    // createFromWizard
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createFromWizard")
    class CreateFromWizardTests {

        @Test
        @DisplayName("Crea lead con status existente y datos correctos")
        void createFromWizard_existingStatus_success() {
            CreateLeadFromWizardRequest request = sampleRequest();
            Lead savedLead = buildSavedLead(request);

            when(agentProfileRepository.findById(10L)).thenReturn(Optional.of(testAgent));
            when(leadStatusRepository.findByName("Nuevo")).thenReturn(Optional.of(defaultStatus));
            when(leadRepository.save(any(Lead.class))).thenReturn(savedLead);
            when(leadInteractionRepository.findByLeadIdOrderByCreatedAtDesc(any())).thenReturn(Collections.emptyList());

            LeadResponse response = leadService.createFromWizard(request);

            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo("John Doe");
            assertThat(response.agentId()).isEqualTo(10L);
            assertThat(response.status()).isEqualTo("Nuevo");
            assertThat(response.source()).isEqualTo("sell_wizard");

            verify(leadRepository).save(any(Lead.class));
            verify(leadStatusRepository, never()).save(any()); // No se creó un status nuevo
        }

        @Test
        @DisplayName("Crea lead y crea status por defecto cuando no existe")
        void createFromWizard_statusNotExists_createsDefault() {
            CreateLeadFromWizardRequest request = sampleRequest();
            Lead savedLead = buildSavedLead(request);

            when(agentProfileRepository.findById(10L)).thenReturn(Optional.of(testAgent));
            when(leadStatusRepository.findByName("Nuevo")).thenReturn(Optional.empty());
            when(leadStatusRepository.findByNameIncludingDeleted("Nuevo")).thenReturn(Optional.empty());
            when(leadStatusRepository.saveAndFlush(any(LeadStatus.class))).thenReturn(defaultStatus);
            when(leadRepository.save(any(Lead.class))).thenReturn(savedLead);
            when(leadInteractionRepository.findByLeadIdOrderByCreatedAtDesc(any())).thenReturn(Collections.emptyList());

            LeadResponse response = leadService.createFromWizard(request);

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo("Nuevo");
            verify(leadStatusRepository).saveAndFlush(any(LeadStatus.class));
        }

        @Test
        @DisplayName("Crea lead y restaura status soft-deleted cuando existe")
        void createFromWizard_softDeletedStatus_restores() {
            CreateLeadFromWizardRequest request = sampleRequest();
            Lead savedLead = buildSavedLead(request);

            LeadStatus deletedStatus = LeadStatus.builder()
                    .name("Nuevo")
                    .color("#3b82f6")
                    .displayOrder(0)
                    .active(false)
                    .build();
            deletedStatus.setId(1L);
            deletedStatus.softDelete();

            when(agentProfileRepository.findById(10L)).thenReturn(Optional.of(testAgent));
            when(leadStatusRepository.findByName("Nuevo")).thenReturn(Optional.empty());
            when(leadStatusRepository.findByNameIncludingDeleted("Nuevo")).thenReturn(Optional.of(deletedStatus));
            when(leadStatusRepository.save(deletedStatus)).thenReturn(defaultStatus);
            when(leadRepository.save(any(Lead.class))).thenReturn(savedLead);
            when(leadInteractionRepository.findByLeadIdOrderByCreatedAtDesc(any())).thenReturn(Collections.emptyList());

            LeadResponse response = leadService.createFromWizard(request);

            assertThat(response).isNotNull();
            assertThat(deletedStatus.getDeletedAt()).isNull(); // restored
            verify(leadStatusRepository).save(deletedStatus);
        }

        @Test
        @DisplayName("Crea lead con manejo de race condition en status")
        void createFromWizard_concurrencyConflict_retrys() {
            CreateLeadFromWizardRequest request = sampleRequest();
            Lead savedLead = buildSavedLead(request);

            when(agentProfileRepository.findById(10L)).thenReturn(Optional.of(testAgent));
            when(leadStatusRepository.findByName("Nuevo")).thenReturn(Optional.empty());
            when(leadStatusRepository.findByNameIncludingDeleted("Nuevo"))
                    .thenReturn(Optional.empty())  // first call
                    .thenReturn(Optional.of(defaultStatus)); // second call after conflict
            when(leadStatusRepository.saveAndFlush(any(LeadStatus.class)))
                    .thenThrow(new DataIntegrityViolationException("unique constraint"));
            when(leadRepository.save(any(Lead.class))).thenReturn(savedLead);
            when(leadInteractionRepository.findByLeadIdOrderByCreatedAtDesc(any())).thenReturn(Collections.emptyList());

            LeadResponse response = leadService.createFromWizard(request);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Agente no encontrado → ResourceNotFoundException")
        void createFromWizard_agentNotFound_throwsException() {
            when(agentProfileRepository.findById(999L))
                    .thenReturn(Optional.empty());

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

            assertThatThrownBy(() -> leadService.createFromWizard(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("Metadata contiene todos los campos del wizard")
        void createFromWizard_metadataContainsWizardFields() {
            CreateLeadFromWizardRequest request = sampleRequest();

            when(agentProfileRepository.findById(10L)).thenReturn(Optional.of(testAgent));
            when(leadStatusRepository.findByName("Nuevo")).thenReturn(Optional.of(defaultStatus));
            when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> {
                Lead l = inv.getArgument(0);
                l.setId(1L);
                l.setCreatedAt(LocalDateTime.now());
                return l;
            });
            when(leadInteractionRepository.findByLeadIdOrderByCreatedAtDesc(any())).thenReturn(Collections.emptyList());

            LeadResponse response = leadService.createFromWizard(request);

            assertThat(response.metadata()).containsKey("address");
            assertThat(response.metadata()).containsKey("propertyType");
            assertThat(response.metadata()).containsKey("category");
            assertThat(response.metadata().get("category")).isEqualTo("SALE");
        }

        @Test
        @DisplayName("Notes contienen la categoría correctamente (SALE → Venta)")
        void createFromWizard_notesContainSaleCategory() {
            CreateLeadFromWizardRequest request = sampleRequest();

            when(agentProfileRepository.findById(10L)).thenReturn(Optional.of(testAgent));
            when(leadStatusRepository.findByName("Nuevo")).thenReturn(Optional.of(defaultStatus));
            when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> {
                Lead l = inv.getArgument(0);
                l.setId(1L);
                l.setCreatedAt(LocalDateTime.now());
                return l;
            });
            when(leadInteractionRepository.findByLeadIdOrderByCreatedAtDesc(any())).thenReturn(Collections.emptyList());

            LeadResponse response = leadService.createFromWizard(request);

            assertThat(response.notes()).contains("Venta");
        }

        @Test
        @DisplayName("Notes contienen la categoría correctamente (RENT → Alquiler)")
        void createFromWizard_notesContainRentCategory() {
            CreateLeadFromWizardRequest rentRequest = new CreateLeadFromWizardRequest(
                    10L,
                    "Jane", "Smith",
                    "+595981111111", "jane@example.com",
                    "Av. Test 456", null, null,
                    PropertyType.APARTMENT, PropertyCategory.RENT,
                    null, null, null,
                    null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null, null,
                    null, null,
                    null, null,
                    null
            );

            when(agentProfileRepository.findById(10L)).thenReturn(Optional.of(testAgent));
            when(leadStatusRepository.findByName("Nuevo")).thenReturn(Optional.of(defaultStatus));
            when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> {
                Lead l = inv.getArgument(0);
                l.setId(2L);
                l.setCreatedAt(LocalDateTime.now());
                return l;
            });
            when(leadInteractionRepository.findByLeadIdOrderByCreatedAtDesc(any())).thenReturn(Collections.emptyList());

            LeadResponse response = leadService.createFromWizard(rentRequest);

            assertThat(response.notes()).contains("Alquiler");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // getLeadsByAgent
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getLeadsByAgent")
    class GetLeadsByAgentTests {

        @Test
        @DisplayName("Retorna página de leads del agente")
        void getLeadsByAgent_returnsPage() {
            Lead lead = buildSavedLead(sampleRequest());
            Page<Lead> page = new PageImpl<>(List.of(lead), PageRequest.of(0, 20), 1);

            when(leadRepository.findByAgentId(eq(10L), any())).thenReturn(page);

            Page<LeadResponse> result = leadService.getLeadsByAgent(10L, PageRequest.of(0, 20));

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // getById
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getById")
    class GetByIdTests {

        @Test
        @DisplayName("Retorna lead por ID con sus interacciones mapeadas")
        void getById_withInteractions_returnsMappedInteractions() {
            Lead lead = buildSavedLead(sampleRequest());
            com.openroof.openroof.model.lead.LeadInteraction interaction = com.openroof.openroof.model.lead.LeadInteraction.builder()
                    .type(com.openroof.openroof.model.enums.InteractionType.CALL)
                    .subject("Llamada inicial")
                    .note("El cliente no atendió")
                    .build();
            interaction.setId(1L);
            interaction.setCreatedAt(LocalDateTime.now());

            when(leadRepository.findWithDetailsById(1L)).thenReturn(Optional.of(lead));
            when(leadInteractionRepository.findByLeadIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(interaction));

            LeadResponse response = leadService.getById(1L);

            assertThat(response).isNotNull();
            assertThat(response.interactions()).hasSize(1);
            assertThat(response.interactions().get(0).subject()).isEqualTo("Llamada inicial");
        }

        @Test
        @DisplayName("Lead no encontrado → ResourceNotFoundException")
        void getById_notFound_throwsException() {
            when(leadRepository.findWithDetailsById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> leadService.getById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // countByAgent
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("countByAgent")
    class CountByAgentTests {

        @Test
        @DisplayName("Retorna conteo de leads del agente")
        void countByAgent_returnsCount() {
            when(leadRepository.countByAgentId(10L)).thenReturn(7L);

            long count = leadService.countByAgent(10L);

            assertThat(count).isEqualTo(7L);
        }
    }
}
