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

        Lead lead1 = Lead.builder()
                .agent(testAgent)
                .status(testStatus)
                .name("Lead 1")
                .email("lead1@test.com")
                .source("wizard")
                .build();
        leadRepository.save(lead1);

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
        Page<Lead> result = leadRepository.findByAgentId(testAgent.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Lead 1");
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
        List<Lead> leads = leadRepository.findAll();
        Long leadId = leads.get(0).getId();
        Long userId = testAgent.getUser().getId();

        boolean exists = leadRepository.existsByIdAndAgent_User_Id(leadId, userId);
        assertThat(exists).isTrue();

        boolean notExists = leadRepository.existsByIdAndAgent_User_Id(leadId, 999L);
        assertThat(notExists).isFalse();
    }
}
