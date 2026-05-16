package com.openroof.openroof.service;

import com.openroof.openroof.dto.agent.AgentRatingSummaryResponse;
import com.openroof.openroof.dto.agent.AgentReviewResponse;
import com.openroof.openroof.dto.agent.CreateAgentReviewRequest;
import com.openroof.openroof.exception.ConflictException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.AgentReviewMapper;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.agent.AgentReview;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.AgentReviewRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentReviewService")
class AgentReviewServiceTest {

    @Mock private AgentReviewRepository reviewRepository;
    @Mock private AgentProfileRepository agentProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private AgentReviewMapper reviewMapper;

    private AgentReviewService service;

    private User reviewer;
    private User agentUser;
    private AgentProfile agent;

    @BeforeEach
    void setUp() {
        service = new AgentReviewService(reviewRepository, agentProfileRepository, userRepository, propertyRepository, reviewMapper);

        reviewer = User.builder().name("Reviewer").email("reviewer@test.com").role(UserRole.USER).build();
        reviewer.setId(10L);

        agentUser = User.builder().name("Agent User").email("agent@test.com").role(UserRole.USER).build();
        agentUser.setId(20L);

        agent = AgentProfile.builder().user(agentUser).build();
        agent.setId(100L);
    }

    // ─── createReview ─────────────────────────────────────────────────────────

    @Test
    void createReview_selfReview_throwsConflict() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(5, "great", null);
        AgentProfile selfAgent = AgentProfile.builder().user(reviewer).build();
        selfAgent.setId(100L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(reviewer));
        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(selfAgent));

        assertThatThrownBy(() -> service.createReview(100L, 10L, req))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createReview_duplicate_throwsConflict() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(5, "great", null);
        when(userRepository.findById(10L)).thenReturn(Optional.of(reviewer));
        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(agent));
        when(reviewRepository.existsByAgent_IdAndUser_Id(100L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.createReview(100L, 10L, req))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createReview_concurrentInsert_translatedToConflict() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(5, "great", null);
        when(userRepository.findById(10L)).thenReturn(Optional.of(reviewer));
        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(agent));
        when(reviewRepository.existsByAgent_IdAndUser_Id(100L, 10L)).thenReturn(false);
        when(reviewMapper.toEntity(any(), any(), any(), any())).thenReturn(new AgentReview());
        when(reviewRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        assertThatThrownBy(() -> service.createReview(100L, 10L, req))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createReview_success_recalculatesRatingIncrementally() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(4, "ok", null);
        AgentReview savedReview = AgentReview.builder().agent(agent).build();
        savedReview.setUser(reviewer);
        savedReview.setId(500L);
        savedReview.setRating(4);
        AgentReviewResponse expectedResponse = new AgentReviewResponse(
                500L, 100L, 10L, "Reviewer", null, null, null, 4, "ok",
                LocalDateTime.now(), LocalDateTime.now(), true);

        when(userRepository.findById(10L)).thenReturn(Optional.of(reviewer));
        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(agent));
        when(reviewRepository.existsByAgent_IdAndUser_Id(100L, 10L)).thenReturn(false);
        when(reviewMapper.toEntity(any(), any(), any(), any())).thenReturn(savedReview);
        when(reviewRepository.saveAndFlush(savedReview)).thenReturn(savedReview);
        when(reviewMapper.toResponse(eq(savedReview), eq(10L))).thenReturn(expectedResponse);

        AgentReviewResponse res = service.createReview(100L, 10L, req);

        assertThat(res.id()).isEqualTo(500L);
        assertThat(res.rating()).isEqualTo(4);
        verify(agentProfileRepository).save(agent);
        // fórmula incremental: (0 * 0 + 4) / 1 = 4.00
        assertThat(agent.getTotalReviews()).isEqualTo(1);
        assertThat(agent.getAvgRating()).isEqualByComparingTo("4.00");
    }

    @Test
    void createReview_agentNotFound_throws() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(reviewer));
        when(agentProfileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createReview(999L, 10L,
                new CreateAgentReviewRequest(5, "ok", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── updateReview ─────────────────────────────────────────────────────────

    @Test
    void updateReview_byOwner_ok() {
        AgentReview review = AgentReview.builder().agent(agent).build();
        review.setUser(reviewer);
        review.setRating(3);
        review.setId(1L);
        AgentReviewResponse expectedResponse = new AgentReviewResponse(
                1L, 100L, 10L, "Reviewer", null, null, null, 5, "updated",
                LocalDateTime.now(), LocalDateTime.now(), true);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);
        when(reviewRepository.countByAgent_Id(100L)).thenReturn(1L);
        when(reviewRepository.avgRatingByAgentId(100L)).thenReturn(5.0);
        when(reviewMapper.toResponse(eq(review), eq(10L))).thenReturn(expectedResponse);

        AgentReviewResponse res = service.updateReview(1L, 10L,
                new CreateAgentReviewRequest(5, "updated", null));

        assertThat(res.rating()).isEqualTo(5);
        assertThat(review.getComment()).isEqualTo("updated");
        verify(agentProfileRepository).save(agent);
    }

    @Test
    void updateReview_byNonOwner_throwsAccessDenied() {
        AgentReview review = AgentReview.builder().agent(agent).build();
        review.setUser(agentUser);
        review.setId(1L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> service.updateReview(1L, 10L,
                new CreateAgentReviewRequest(5, "x", null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateReview_notFound_throws() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateReview(999L, 10L,
                new CreateAgentReviewRequest(5, "x", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── deleteReview ─────────────────────────────────────────────────────────

    @Test
    void deleteReview_byOwner_ok() {
        AgentReview review = AgentReview.builder().agent(agent).build();
        review.setUser(reviewer);
        review.setId(1L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.countByAgent_Id(100L)).thenReturn(0L);
        when(reviewRepository.avgRatingByAgentId(100L)).thenReturn(null);

        service.deleteReview(1L, 10L);

        verify(reviewRepository).delete(review);
        verify(agentProfileRepository).save(agent);
    }

    @Test
    void deleteReview_byNonOwner_throwsAccessDenied() {
        AgentReview review = AgentReview.builder().agent(agent).build();
        review.setUser(agentUser);
        review.setId(1L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> service.deleteReview(1L, 10L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteReview_byAdmin_throwsAccessDenied() {
        // Admin que no es el autor no puede borrar via este endpoint
        User admin = User.builder().email("admin@test.com").role(UserRole.ADMIN).build();
        admin.setId(99L);
        AgentReview review = AgentReview.builder().agent(agent).build();
        review.setUser(reviewer);
        review.setId(1L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> service.deleteReview(1L, 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteReview_notFound_throws() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteReview(999L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── getReviews ───────────────────────────────────────────────────────────

    @Test
    void getReviews_agentNotFound_throws() {
        when(agentProfileRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> service.getReviews(404L, null, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getReviews_returnsPage() {
        AgentReview r = AgentReview.builder().agent(agent).build();
        r.setUser(reviewer);
        r.setId(1L);
        r.setRating(4);
        AgentReviewResponse expected = new AgentReviewResponse(
                1L, 100L, 10L, "Reviewer", null, null, null, 4, null,
                LocalDateTime.now(), LocalDateTime.now(), false);

        when(agentProfileRepository.existsById(100L)).thenReturn(true);
        when(reviewRepository.findByAgent_Id(eq(100L), any())).thenReturn(new PageImpl<>(List.of(r)));
        when(reviewMapper.toResponse(eq(r), eq((Long) null))).thenReturn(expected);

        Page<AgentReviewResponse> page = service.getReviews(100L, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).rating()).isEqualTo(4);
    }

    // ─── getRatingSummary ─────────────────────────────────────────────────────

    @Test
    void getRatingSummary_agentNotFound_throws() {
        when(agentProfileRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRatingSummary(404L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getRatingSummary_returnsSummary() {
        agent.setAvgRating(new BigDecimal("4.50"));
        agent.setTotalReviews(10);
        AgentRatingSummaryResponse expected = new AgentRatingSummaryResponse(
                new BigDecimal("4.50"), 10, Map.of(), List.of());

        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(agent));
        when(reviewRepository.countRatingDistributionByAgentId(100L)).thenReturn(List.of());
        when(reviewRepository.findTop5ByAgent_IdOrderByCreatedAtDesc(100L)).thenReturn(List.of());
        when(reviewMapper.toSummaryResponse(eq(agent), any(), any())).thenReturn(expected);

        AgentRatingSummaryResponse res = service.getRatingSummary(100L, null);

        assertThat(res.avgRating()).isEqualByComparingTo("4.50");
        assertThat(res.totalReviews()).isEqualTo(10);
    }
}

