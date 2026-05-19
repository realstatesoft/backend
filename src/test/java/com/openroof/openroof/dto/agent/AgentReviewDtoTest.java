package com.openroof.openroof.dto.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DTOs de AgentReview — estructura y campos")
class AgentReviewDtoTest {

    // ─── AgentReviewResponse ────────────────────────────────────────────────

    @Test
    @DisplayName("AgentReviewResponse almacena todos los campos correctamente")
    void agentReviewResponse_storesAllFields() {
        LocalDateTime now = LocalDateTime.now();
        AgentReviewResponse dto = new AgentReviewResponse(
                1L, 10L, 20L, "Juan Pérez", "https://cdn.test/avatar.jpg",
                5L, "Calle Falsa 123",
                4, "Muy buen agente, atento y profesional.",
                now, now, true);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.agentId()).isEqualTo(10L);
        assertThat(dto.reviewerId()).isEqualTo(20L);
        assertThat(dto.reviewerName()).isEqualTo("Juan Pérez");
        assertThat(dto.reviewerAvatarUrl()).isEqualTo("https://cdn.test/avatar.jpg");
        assertThat(dto.propertyId()).isEqualTo(5L);
        assertThat(dto.propertyAddress()).isEqualTo("Calle Falsa 123");
        assertThat(dto.rating()).isEqualTo(4);
        assertThat(dto.comment()).isEqualTo("Muy buen agente, atento y profesional.");
        assertThat(dto.createdAt()).isEqualTo(now);
        assertThat(dto.updatedAt()).isEqualTo(now);
        assertThat(dto.isOwn()).isTrue();
    }

    @Test
    @DisplayName("AgentReviewResponse acepta campos opcionales null")
    void agentReviewResponse_acceptsNullOptionalFields() {
        LocalDateTime now = LocalDateTime.now();
        AgentReviewResponse dto = new AgentReviewResponse(
                2L, 10L, 20L, "Ana García", null,
                null, null,
                5, "Excelente profesional, muy recomendable.",
                now, now, false);

        assertThat(dto.reviewerAvatarUrl()).isNull();
        assertThat(dto.propertyId()).isNull();
        assertThat(dto.propertyAddress()).isNull();
        assertThat(dto.isOwn()).isFalse();
    }

    @Test
    @DisplayName("AgentReviewResponse es un record con igualdad por valor")
    void agentReviewResponse_equalsByValue() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 10, 0);
        AgentReviewResponse a = new AgentReviewResponse(1L, 10L, 20L, "Juan", null, null, null, 5, "Comentario largo suficiente.", now, now, false);
        AgentReviewResponse b = new AgentReviewResponse(1L, 10L, 20L, "Juan", null, null, null, 5, "Comentario largo suficiente.", now, now, false);

        assertThat(a).isEqualTo(b);
    }

    // ─── AgentRatingSummaryResponse ─────────────────────────────────────────

    @Test
    @DisplayName("AgentRatingSummaryResponse almacena todos los campos correctamente")
    void agentRatingSummaryResponse_storesAllFields() {
        LocalDateTime now = LocalDateTime.now();
        Map<Integer, Long> distribution = Map.of(1, 2L, 2, 5L, 3, 10L, 4, 20L, 5, 30L);

        AgentReviewResponse review = new AgentReviewResponse(
                1L, 10L, 20L, "Juan", null, null, null,
                5, "Excelente atención del agente.", now, now, false);

        AgentRatingSummaryResponse summary = new AgentRatingSummaryResponse(
                new BigDecimal("4.55"),
                67,
                distribution,
                List.of(review));

        assertThat(summary.avgRating()).isEqualByComparingTo("4.55");
        assertThat(summary.totalReviews()).isEqualTo(67);
        assertThat(summary.ratingDistribution()).containsEntry(5, 30L);
        assertThat(summary.ratingDistribution()).containsEntry(1, 2L);
        assertThat(summary.latestReviews()).hasSize(1);
        assertThat(summary.latestReviews().get(0).reviewerName()).isEqualTo("Juan");
    }

    @Test
    @DisplayName("AgentRatingSummaryResponse acepta lista vacía de latestReviews")
    void agentRatingSummaryResponse_acceptsEmptyLatestReviews() {
        AgentRatingSummaryResponse summary = new AgentRatingSummaryResponse(
                BigDecimal.ZERO, 0, Map.of(), List.of());

        assertThat(summary.totalReviews()).isZero();
        assertThat(summary.latestReviews()).isEmpty();
        assertThat(summary.ratingDistribution()).isEmpty();
    }

    @Test
    @DisplayName("AgentRatingSummaryResponse latestReviews contiene máximo 3 para preview")
    void agentRatingSummaryResponse_latestReviewsHasAtMostThree() {
        LocalDateTime now = LocalDateTime.now();
        List<AgentReviewResponse> latest = List.of(
                new AgentReviewResponse(1L, 10L, 1L, "A", null, null, null, 5, "Primera reseña de ejemplo.", now, now, false),
                new AgentReviewResponse(2L, 10L, 2L, "B", null, null, null, 4, "Segunda reseña de ejemplo.", now, now, false),
                new AgentReviewResponse(3L, 10L, 3L, "C", null, null, null, 3, "Tercera reseña de ejemplo.", now, now, false));

        AgentRatingSummaryResponse summary = new AgentRatingSummaryResponse(
                new BigDecimal("4.00"), 3, Map.of(3, 1L, 4, 1L, 5, 1L), latest);

        assertThat(summary.latestReviews()).hasSize(3);
    }
}
