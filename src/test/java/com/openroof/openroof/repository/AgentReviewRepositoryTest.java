package com.openroof.openroof.repository;

import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.agent.AgentReview;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AgentReviewRepositoryTest {

    @Autowired
    private AgentReviewRepository agentReviewRepository;

    @Autowired
    private AgentProfileRepository agentProfileRepository;

    @Autowired
    private UserRepository userRepository;

    private AgentProfile testAgent;
    private User reviewer1;
    private User reviewer2;

    @BeforeEach
    void setUp() {
        User agentUser = User.builder()
                .email("agent-review-test@test.com")
                .passwordHash("hash")
                .name("Agent Under Review")
                .role(UserRole.AGENT)
                .build();
        agentUser = userRepository.save(agentUser);

        testAgent = AgentProfile.builder()
                .user(agentUser)
                .companyName("Test Realty")
                .avgRating(BigDecimal.ZERO)
                .totalReviews(0)
                .build();
        testAgent = agentProfileRepository.save(testAgent);

        reviewer1 = User.builder()
                .email("reviewer1-review-test@test.com")
                .passwordHash("hash")
                .name("Reviewer One")
                .role(UserRole.USER)
                .build();
        reviewer1 = userRepository.save(reviewer1);

        reviewer2 = User.builder()
                .email("reviewer2-review-test@test.com")
                .passwordHash("hash")
                .name("Reviewer Two")
                .role(UserRole.USER)
                .build();
        reviewer2 = userRepository.save(reviewer2);

        AgentReview review1 = AgentReview.builder().agent(testAgent).build();
        review1.setUser(reviewer1);
        review1.setRating(5);
        review1.setComment("Excelente agente");
        agentReviewRepository.save(review1);

        AgentReview review2 = AgentReview.builder().agent(testAgent).build();
        review2.setUser(reviewer2);
        review2.setRating(3);
        review2.setComment("Buena experiencia");
        agentReviewRepository.save(review2);
    }

    @Test
    @DisplayName("findByAgentId debe retornar reviews paginadas del agente")
    void findByAgentId_returnsPagedReviews() {
        Page<AgentReview> result = agentReviewRepository.findByAgentId(
                testAgent.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(r -> r.getAgent().getId())
                .containsOnly(testAgent.getId());
    }

    @Test
    @DisplayName("findByAgentId debe retornar página vacía para agente sin reviews")
    void findByAgentId_returnsEmptyForUnknownAgent() {
        Page<AgentReview> result = agentReviewRepository.findByAgentId(
                Long.MAX_VALUE, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("findByAgentIdAndUserId debe encontrar la review existente")
    void findByAgentIdAndUserId_returnsReviewWhenExists() {
        Optional<AgentReview> result = agentReviewRepository.findByAgentIdAndUserId(
                testAgent.getId(), reviewer1.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getRating()).isEqualTo(5);
        assertThat(result.get().getComment()).isEqualTo("Excelente agente");
    }

    @Test
    @DisplayName("findByAgentIdAndUserId debe retornar vacío cuando no existe")
    void findByAgentIdAndUserId_returnsEmptyWhenNotExists() {
        Optional<AgentReview> result = agentReviewRepository.findByAgentIdAndUserId(
                testAgent.getId(), Long.MAX_VALUE);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByAgentIdAndUserId debe retornar true cuando existe la review")
    void existsByAgentIdAndUserId_returnsTrueWhenExists() {
        boolean exists = agentReviewRepository.existsByAgentIdAndUserId(
                testAgent.getId(), reviewer1.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByAgentIdAndUserId debe retornar false cuando no existe")
    void existsByAgentIdAndUserId_returnsFalseWhenNotExists() {
        boolean exists = agentReviewRepository.existsByAgentIdAndUserId(
                testAgent.getId(), Long.MAX_VALUE);

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("countRatingDistributionByAgentId debe agrupar correctamente los ratings")
    void countRatingDistributionByAgentId_returnsCorrectDistribution() {
        List<AgentReviewRepository.RatingDistribution> distribution =
                agentReviewRepository.countRatingDistributionByAgentId(testAgent.getId());

        assertThat(distribution).hasSize(2);
        assertThat(distribution)
                .extracting(AgentReviewRepository.RatingDistribution::getRating)
                .containsExactlyInAnyOrder(5, 3);
        assertThat(distribution)
                .extracting(AgentReviewRepository.RatingDistribution::getCount)
                .containsOnly(1L);
    }

    @Test
    @DisplayName("findTop5ByAgentIdOrderByCreatedAtDesc debe retornar máximo 5 reviews ordenadas")
    void findTop5ByAgentIdOrderByCreatedAtDesc_returnsAtMostFiveOrderedByDate() {
        // Agregar 4 reviews adicionales para tener 6 en total
        for (int i = 3; i <= 6; i++) {
            User extra = userRepository.save(User.builder()
                    .email("extra" + i + "-review-test@test.com")
                    .passwordHash("hash")
                    .name("Extra Reviewer " + i)
                    .role(UserRole.USER)
                    .build());
            AgentReview extra_review = AgentReview.builder().agent(testAgent).build();
            extra_review.setUser(extra);
            extra_review.setRating(i % 5 + 1);
            extra_review.setComment("Review " + i);
            agentReviewRepository.save(extra_review);
        }

        List<AgentReview> top5 = agentReviewRepository
                .findTop5ByAgentIdOrderByCreatedAtDesc(testAgent.getId());

        assertThat(top5).hasSize(5);
    }
}
