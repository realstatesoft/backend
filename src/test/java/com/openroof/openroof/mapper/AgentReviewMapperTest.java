package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.agent.AgentRatingSummaryResponse;
import com.openroof.openroof.dto.agent.AgentReviewResponse;
import com.openroof.openroof.dto.agent.CreateAgentReviewRequest;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.agent.AgentReview;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AgentReviewMapper")
class AgentReviewMapperTest {

    private AgentReviewMapper mapper;

    private User reviewer;
    private AgentProfile agent;
    private Property property;
    private AgentReview review;

    @BeforeEach
    void setUp() {
        mapper = new AgentReviewMapper();

        User agentUser = User.builder()
                .email("agent@test.com")
                .passwordHash("hash")
                .name("Agent Name")
                .role(UserRole.AGENT)
                .build();
        agentUser.setId(1L);

        agent = AgentProfile.builder()
                .user(agentUser)
                .companyName("Test Realty")
                .avgRating(new BigDecimal("4.50"))
                .totalReviews(10)
                .build();
        agent.setId(10L);

        reviewer = User.builder()
                .email("reviewer@test.com")
                .passwordHash("hash")
                .name("Juan Pérez")
                .role(UserRole.USER)
                .build();
        reviewer.setId(20L);
        reviewer.setAvatarUrl("https://cdn.test/avatar.jpg");

        property = Property.builder()
                .title("Casa en Villa Morra")
                .address("Av. España 1234")
                .owner(agentUser)
                .build();
        property.setId(5L);

        review = AgentReview.builder()
                .agent(agent)
                .property(property)
                .build();
        review.setId(100L);
        review.setUser(reviewer);
        review.setRating(5);
        review.setComment("Excelente agente, muy profesional.");
    }

    // ─── toResponse ────────────────────────────────────────────────────────

    @Test
    @DisplayName("toResponse() mapea todos los campos correctamente")
    void toResponse_mapsAllFields() {
        AgentReviewResponse response = mapper.toResponse(review, 20L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.agentId()).isEqualTo(10L);
        assertThat(response.reviewerId()).isEqualTo(20L);
        assertThat(response.reviewerName()).isEqualTo("Juan Pérez");
        assertThat(response.reviewerAvatarUrl()).isEqualTo("https://cdn.test/avatar.jpg");
        assertThat(response.propertyId()).isEqualTo(5L);
        assertThat(response.propertyAddress()).isEqualTo("Av. España 1234");
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("Excelente agente, muy profesional.");
    }

    @Test
    @DisplayName("toResponse() calcula isOwn=true cuando currentUserId coincide con el reviewer")
    void toResponse_isOwnTrue_whenCurrentUserIsReviewer() {
        AgentReviewResponse response = mapper.toResponse(review, 20L);
        assertThat(response.isOwn()).isTrue();
    }

    @Test
    @DisplayName("toResponse() calcula isOwn=false cuando currentUserId es diferente")
    void toResponse_isOwnFalse_whenCurrentUserIsDifferent() {
        AgentReviewResponse response = mapper.toResponse(review, 99L);
        assertThat(response.isOwn()).isFalse();
    }

    @Test
    @DisplayName("toResponse() calcula isOwn=false cuando currentUserId es null")
    void toResponse_isOwnFalse_whenCurrentUserIdIsNull() {
        AgentReviewResponse response = mapper.toResponse(review, null);
        assertThat(response.isOwn()).isFalse();
    }

    @Test
    @DisplayName("toResponse() maneja property null correctamente")
    void toResponse_handlesNullProperty() {
        AgentReview reviewNoProperty = AgentReview.builder().agent(agent).build();
        reviewNoProperty.setId(101L);
        reviewNoProperty.setUser(reviewer);
        reviewNoProperty.setRating(4);
        reviewNoProperty.setComment("Muy buena atención del agente.");

        AgentReviewResponse response = mapper.toResponse(reviewNoProperty, null);

        assertThat(response.propertyId()).isNull();
        assertThat(response.propertyAddress()).isNull();
    }

    @Test
    @DisplayName("toResponse() maneja reviewer null correctamente")
    void toResponse_handlesNullReviewer() {
        AgentReview reviewNoUser = AgentReview.builder().agent(agent).build();
        reviewNoUser.setId(102L);
        reviewNoUser.setRating(3);
        reviewNoUser.setComment("Comentario sin usuario asignado.");

        AgentReviewResponse response = mapper.toResponse(reviewNoUser, null);

        assertThat(response.reviewerId()).isNull();
        assertThat(response.reviewerName()).isNull();
        assertThat(response.reviewerAvatarUrl()).isNull();
        assertThat(response.isOwn()).isFalse();
    }

    @Test
    @DisplayName("toResponse() maneja agent null correctamente")
    void toResponse_handlesNullAgent() {
        AgentReview reviewNoAgent = new AgentReview();
        reviewNoAgent.setId(103L);
        reviewNoAgent.setUser(reviewer);
        reviewNoAgent.setRating(4);
        reviewNoAgent.setComment("Revisión sin agente asignado válida.");

        AgentReviewResponse response = mapper.toResponse(reviewNoAgent, null);

        assertThat(response.agentId()).isNull();
    }

    // ─── toEntity ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("toEntity() crea la entidad con todos los campos correctamente")
    void toEntity_createsEntityWithAllFields() {
        CreateAgentReviewRequest dto = new CreateAgentReviewRequest(4, "Muy buen profesional y atento.", 5L);

        AgentReview entity = mapper.toEntity(dto, agent, reviewer, property);

        assertThat(entity.getRating()).isEqualTo(4);
        assertThat(entity.getComment()).isEqualTo("Muy buen profesional y atento.");
        assertThat(entity.getAgent()).isEqualTo(agent);
        assertThat(entity.getUser()).isEqualTo(reviewer);
        assertThat(entity.getProperty()).isEqualTo(property);
    }

    @Test
    @DisplayName("toEntity() acepta property null (campo opcional)")
    void toEntity_acceptsNullProperty() {
        CreateAgentReviewRequest dto = new CreateAgentReviewRequest(5, "Excelente trato y muy profesional.", null);

        AgentReview entity = mapper.toEntity(dto, agent, reviewer, null);

        assertThat(entity.getProperty()).isNull();
        assertThat(entity.getRating()).isEqualTo(5);
    }

    // ─── toSummaryResponse ─────────────────────────────────────────────────

    @Test
    @DisplayName("toSummaryResponse() construye el DTO de resumen correctamente")
    void toSummaryResponse_buildsCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        AgentReviewResponse latestReview = new AgentReviewResponse(
                100L, 10L, 20L, "Juan Pérez", null, null, null,
                5, "Excelente agente.", now, now, false);

        Map<Integer, Long> distribution = Map.of(5, 8L, 4, 2L);

        AgentRatingSummaryResponse summary = mapper.toSummaryResponse(agent, List.of(latestReview), distribution);

        assertThat(summary.avgRating()).isEqualByComparingTo("4.50");
        assertThat(summary.totalReviews()).isEqualTo(10);
        assertThat(summary.ratingDistribution()).isEqualTo(distribution);
        assertThat(summary.latestReviews()).hasSize(1);
        assertThat(summary.latestReviews().get(0).reviewerName()).isEqualTo("Juan Pérez");
    }

    @Test
    @DisplayName("toSummaryResponse() acepta lista vacía de latestReviews")
    void toSummaryResponse_acceptsEmptyLatestReviews() {
        AgentRatingSummaryResponse summary = mapper.toSummaryResponse(agent, List.of(), Map.of());

        assertThat(summary.latestReviews()).isEmpty();
        assertThat(summary.ratingDistribution()).isEmpty();
        assertThat(summary.totalReviews()).isEqualTo(10);
    }
}
