package com.openroof.openroof.service;

import com.openroof.openroof.dto.agent.AgentRatingSummaryResponse;
import com.openroof.openroof.dto.agent.AgentReviewResponse;
import com.openroof.openroof.dto.agent.CreateAgentReviewRequest;
import com.openroof.openroof.exception.BadRequestException;
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
import org.mockito.ArgumentCaptor;
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
        service = new AgentReviewService(reviewRepository, agentProfileRepository, userRepository,
                propertyRepository, reviewMapper);

        reviewer = User.builder().name("Reviewer").email("reviewer@test.com").role(UserRole.USER).build();
        reviewer.setId(10L);

        agentUser = User.builder().name("Agent User").email("agent@test.com").role(UserRole.USER).build();
        agentUser.setId(20L);

        agent = AgentProfile.builder().user(agentUser).build();
        agent.setId(100L);
    }

    // --- createReview ---------------------------------------------------------

    @Test
    void createReview_selfReview_throwsBadRequest() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(5, "great", null);
        AgentProfile selfAgent = AgentProfile.builder().user(reviewer).build();
        selfAgent.setId(100L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(reviewer));
        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(selfAgent));

        assertThatThrownBy(() -> service.createReview(100L, 10L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createReview_duplicateReview_throwsConflict() {
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
        AgentReview mappedReview = AgentReview.builder().agent(agent).build();
        mappedReview.setUser(reviewer);
        mappedReview.setRating(4);
        AgentReviewResponse mappedResponse = new AgentReviewResponse(
                500L, 100L, 10L, "Reviewer", null, null, null, 4, "ok",
                LocalDateTime.now(), LocalDateTime.now(), true);

        when(userRepository.findById(10L)).thenReturn(Optional.of(reviewer));
        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(agent));
        when(reviewRepository.existsByAgent_IdAndUser_Id(100L, 10L)).thenReturn(false);
        when(reviewMapper.toEntity(eq(req), eq(agent), eq(reviewer), eq(null))).thenReturn(mappedReview);
        when(reviewRepository.saveAndFlush(eq(mappedReview))).thenAnswer(inv -> {
            AgentReview r = inv.getArgument(0);
            r.setId(500L);
            return r;
        });
        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(agent));
        when(reviewRepository.calculateTotalReviews(100L)).thenReturn(1L);
        when(reviewRepository.calculateAvgRating(100L)).thenReturn(Optional.of(4.0));
        when(reviewMapper.toResponse(eq(mappedReview), eq(10L))).thenReturn(mappedResponse);

        AgentReviewResponse res = service.createReview(100L, 10L, req);

        assertThat(res.id()).isEqualTo(500L);
        assertThat(res.rating()).isEqualTo(4);
        verify(agentProfileRepository).save(agent);
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

    // --- updateReview ---------------------------------------------------------

    @Test
    void updateReview_success() {
        AgentReview review = AgentReview.builder().agent(agent).build();
        review.setUser(reviewer);
        review.setRating(3);
        review.setId(1L);
        AgentReviewResponse mappedResponse = new AgentReviewResponse(
                1L, 100L, 10L, "Reviewer", null, null, null, 5, "updated",
                LocalDateTime.now(), LocalDateTime.now(), true);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);
        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(agent));
        when(reviewRepository.calculateTotalReviews(100L)).thenReturn(1L);
        when(reviewRepository.calculateAvgRating(100L)).thenReturn(Optional.of(5.0));
        when(reviewMapper.toResponse(eq(review), eq(10L))).thenReturn(mappedResponse);

        AgentReviewResponse res = service.updateReview(1L, 10L,
                new CreateAgentReviewRequest(5, "updated", null));

        assertThat(res.rating()).isEqualTo(5);
        assertThat(review.getComment()).isEqualTo("updated");
        verify(agentProfileRepository).save(agent);
    }

    @Test
    void updateReview_notOwner_throwsAccessDenied() {
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

    // --- deleteReview ---------------------------------------------------------

    @Test
    void deleteReview_success() {
        AgentReview review = AgentReview.builder().agent(agent).build();
        review.setUser(reviewer);
        review.setId(1L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(userRepository.findById(10L)).thenReturn(Optional.of(reviewer));
        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(agent));
        when(reviewRepository.calculateTotalReviews(100L)).thenReturn(0L);
        when(reviewRepository.calculateAvgRating(100L)).thenReturn(Optional.empty());

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
        when(userRepository.findById(10L)).thenReturn(Optional.of(reviewer));

        assertThatThrownBy(() -> service.deleteReview(1L, 10L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteReview_adminCanDeleteAnyReview() {
        User admin = User.builder().email("admin@test.com").role(UserRole.ADMIN).build();
        admin.setId(99L);
        AgentReview review = AgentReview.builder().agent(agent).build();
        review.setUser(reviewer);
        review.setId(1L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(agent));
        when(reviewRepository.calculateTotalReviews(100L)).thenReturn(0L);
        when(reviewRepository.calculateAvgRating(100L)).thenReturn(Optional.empty());
        
        service.deleteReview(1L, 99L);
        
        verify(reviewRepository).delete(review);
        verify(agentProfileRepository).save(agent);
    }

    @Test
    void deleteReview_notFound_throws() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteReview(999L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- getReviews -----------------------------------------------------------

    @Test
    void getReviews_agentNotFound_throws() {
        when(agentProfileRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> service.getReviews(404L, null, PageRequest.of(0, 10), null))
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

        Page<AgentReviewResponse> page = service.getReviews(100L, null, PageRequest.of(0, 10), null);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).rating()).isEqualTo(4);
    }

    @Test
    void getReviews_withRatingFilter_usesFilteredQuery() {
        AgentReview r = AgentReview.builder().agent(agent).build();
        r.setUser(reviewer);
        r.setId(2L);
        r.setRating(5);
        AgentReviewResponse expected = new AgentReviewResponse(
                2L, 100L, 10L, "Reviewer", null, null, null, 5, "great",
                LocalDateTime.now(), LocalDateTime.now(), false);

        when(agentProfileRepository.existsById(100L)).thenReturn(true);
        when(reviewRepository.findByAgent_IdAndRating(eq(100L), eq(5), any()))
                .thenReturn(new PageImpl<>(List.of(r)));
        when(reviewMapper.toResponse(eq(r), eq((Long) null))).thenReturn(expected);

        Page<AgentReviewResponse> page = service.getReviews(100L, null, PageRequest.of(0, 10), 5);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).rating()).isEqualTo(5);
        verify(reviewRepository).findByAgent_IdAndRating(eq(100L), eq(5), any());
    }

    @Test
    void getReviews_withInvalidRating_throwsBadRequest() {
        when(agentProfileRepository.existsById(100L)).thenReturn(true);

        assertThatThrownBy(() -> service.getReviews(100L, null, PageRequest.of(0, 10), 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("between 1 and 5");
    }

    // --- getRatingSummary -----------------------------------------------------

    @Test
    void getRatingSummary_agentNotFound_throws() {
        when(agentProfileRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRatingSummary(404L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getRatingSummary_correctDistribution() {
        agent.setAvgRating(new BigDecimal("4.67"));
        agent.setTotalReviews(3);
        
        AgentReviewRepository.RatingDistribution dist4 = new AgentReviewRepository.RatingDistribution() {
            public Integer getRating() { return 4; }
            public Long getCount() { return 1L; }
        };
        AgentReviewRepository.RatingDistribution dist5 = new AgentReviewRepository.RatingDistribution() {
            public Integer getRating() { return 5; }
            public Long getCount() { return 2L; }
        };
        AgentRatingSummaryResponse expected = new AgentRatingSummaryResponse(new BigDecimal("4.67"), 3, Map.of(4, 1L, 5, 2L), List.of());

        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(agent));
        when(reviewRepository.countRatingDistributionByAgentId(100L)).thenReturn(List.of(dist4, dist5));
        when(reviewMapper.toSummaryResponse(eq(agent), any(), any())).thenReturn(expected);

        AgentRatingSummaryResponse res = service.getRatingSummary(100L, null);
        assertThat(res.avgRating()).isEqualByComparingTo("4.67");
        assertThat(res.ratingDistribution().get(4)).isEqualTo(1L);
        assertThat(res.ratingDistribution().get(5)).isEqualTo(2L);

        ArgumentCaptor<Map> distCaptor = ArgumentCaptor.forClass(Map.class);
        verify(reviewMapper).toSummaryResponse(eq(agent), any(), distCaptor.capture());
        assertThat(distCaptor.getValue()).containsAllEntriesOf(Map.of(4, 1L, 5, 2L));
    }

    // --- getMyReview ----------------------------------------------------------

    @Test
    void getMyReview_found() {
        AgentReview review = AgentReview.builder().agent(agent).build();
        review.setUser(reviewer);
        review.setId(1L);
        review.setRating(5);
        AgentReviewResponse expected = new AgentReviewResponse(
                1L, 100L, 10L, "Reviewer", null, null, null, 5, "great",
                LocalDateTime.now(), LocalDateTime.now(), true);

        when(reviewRepository.findByAgent_IdAndUser_Id(100L, 10L)).thenReturn(Optional.of(review));
        when(reviewMapper.toResponse(eq(review), eq(10L))).thenReturn(expected);

        AgentReviewResponse result = service.getMyReview(100L, 10L);

        assertThat(result).isNotNull();
        assertThat(result.rating()).isEqualTo(5);
    }

    @Test
    void getMyReview_notFound_returnsNull() {
        when(reviewRepository.findByAgent_IdAndUser_Id(100L, 10L)).thenReturn(Optional.empty());

        AgentReviewResponse result = service.getMyReview(100L, 10L);

        assertThat(result).isNull();
    }
}
