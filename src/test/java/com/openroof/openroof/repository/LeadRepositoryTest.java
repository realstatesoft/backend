package com.openroof.openroof.repository;

import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.lead.Lead;
import com.openroof.openroof.model.lead.LeadStatus;
import com.openroof.openroof.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class LeadRepositoryTest {

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgentProfileRepository agentProfileRepository;

    @Autowired
    private LeadStatusRepository leadStatusRepository;

    private AgentProfile testAgent;
    private LeadStatus testStatus;
    private Lead lead1;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("agent@test.com")
                .passwordHash("hash")
                .name("Agent Name")
                .role(com.openroof.openroof.model.enums.UserRole.AGENT)
                .build();
        user = userRepository.save(user);

        testAgent = AgentProfile.builder()
                .user(user)
                .companyName("Test Realty")
                .avgRating(BigDecimal.ZERO)
                .totalReviews(0)
                .build();
        testAgent = agentProfileRepository.save(testAgent);

        testStatus = LeadStatus.builder()
                .name("Nuevo")
                .color("#3b82f6")
                .displayOrder(0)
                .active(true)
                .build();
        testStatus = leadStatusRepository.save(testStatus);

        lead1 = Lead.builder()
                .agent(testAgent)
                .status(testStatus)
                .name("Lead 1")
                .email("lead1@test.com")
                .source("wizard")
                .build();
        lead1 = leadRepository.save(lead1);

        Lead lead2 = Lead.builder()
                .agent(testAgent)
                .status(testStatus)
                .name("Lead 2")
                .email("lead2@test.com")
                .source("wizard")
                .build();
        leadRepository.save(lead2);
    }

    @Test
    @DisplayName("Debe encontrar leads por Agent ID con paginación")
    void findByAgentId_returnsPage() {
        Page<Lead> result = leadRepository.findByAgentId(testAgent.getId(), 
            PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("name")));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Lead::getName)
                .containsExactlyInAnyOrder("Lead 1", "Lead 2");
    }

    @Test
    @DisplayName("Debe contar leads correctamente por Agent ID")
    void countByAgentId_returnsCorrectCount() {
        long count = leadRepository.countByAgentId(testAgent.getId());
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Debe verificar si un lead pertenece a un usuario (agente)")
    void existsByIdAndAgent_User_Id_works() {
        Long leadId = lead1.getId();
        Long userId = testAgent.getUser().getId();
 
        boolean exists = leadRepository.existsByIdAndAgent_User_Id(leadId, userId);
        assertThat(exists).isTrue();
 
        boolean notExists = leadRepository.existsByIdAndAgent_User_Id(leadId, Long.MAX_VALUE);
        assertThat(notExists).isFalse();
    }
}
