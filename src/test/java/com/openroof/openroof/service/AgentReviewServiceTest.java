package com.openroof.openroof.service;

import com.openroof.openroof.dto.agent.AgentReviewResponse;
import com.openroof.openroof.dto.agent.AgentReviewSummaryResponse;
import com.openroof.openroof.dto.agent.CreateAgentReviewRequest;
import com.openroof.openroof.dto.agent.UpdateAgentReviewRequest;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ConflictException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentReviewService")
class AgentReviewServiceTest {

    @Mock private AgentReviewRepository reviewRepository;
    @Mock private AgentProfileRepository agentProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private PropertyRepository propertyRepository;

    private AgentReviewService service;

    private User reviewer;
    private User agentUser;
    private AgentProfile agent;

    @BeforeEach
    void setUp() {
        service = new AgentReviewService(reviewRepository, agentProfileRepository, userRepository, propertyRepository);

        reviewer = User.builder().name("Reviewer").email("reviewer@test.com").role(UserRole.USER).build();
        reviewer.setId(10L);

        agentUser = User.builder().name("Agent User").email("agent@test.com").role(UserRole.USER).build();
        agentUser.setId(20L);

        agent = AgentProfile.builder().user(agentUser).build();
        agent.setId(100L);
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    void create_selfReview_throwsBadRequest() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(5, "great", null);
        AgentProfile selfAgent = AgentProfile.builder().user(reviewer).build();
        selfAgent.setId(100L);
        when(userRepository.findByEmail("reviewer@test.com")).thenReturn(Optional.of(reviewer));
        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(selfAgent));

        assertThatThrownBy(() -> service.create(100L, "reviewer@test.com", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("vos mismo");
    }

    @Test
    void create_duplicate_throwsConflict() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(5, "great", null);
        when(userRepository.findByEmail("reviewer@test.com")).thenReturn(Optional.of(reviewer));
        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(agent));
        when(reviewRepository.existsByAgent_IdAndUser_Id(100L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(100L, "reviewer@test.com", req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Ya reseñaste");
    }

    @Test
    void create_concurrentInsert_translatedToConflict() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(5, "great", null);
        when(userRepository.findByEmail("reviewer@test.com")).thenReturn(Optional.of(reviewer));
        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(agent));
        when(reviewRepository.existsByAgent_IdAndUser_Id(100L, 10L)).thenReturn(false);
        when(reviewRepository.saveAndFlush(any(AgentReview.class)))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        assertThatThrownBy(() -> service.create(100L, "reviewer@test.com", req))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_success_recalculatesRating() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(4, "ok", null);
        when(userRepository.findByEmail("reviewer@test.com")).thenReturn(Optional.of(reviewer));
        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(agent));
        when(reviewRepository.existsByAgent_IdAndUser_Id(100L, 10L)).thenReturn(false);
        when(reviewRepository.saveAndFlush(any(AgentReview.class))).thenAnswer(inv -> {
            AgentReview r = inv.getArgument(0);
            r.setId(500L);
            return r;
        });
        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(agent));
        when(reviewRepository.calculateTotalReviews(100L)).thenReturn(1L);
        when(reviewRepository.calculateAvgRating(100L)).thenReturn(Optional.of(4.0));

        AgentReviewResponse res = service.create(100L, "reviewer@test.com", req);

        assertThat(res.id()).isEqualTo(500L);
        assertThat(res.rating()).isEqualTo(4);
        verify(agentProfileRepository).save(agent);
        assertThat(agent.getTotalReviews()).isEqualTo(1);
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    void update_byOwner_ok() {
        AgentReview review = AgentReview.builder().agent(agent).build();
        review.setUser(reviewer);
        review.setRating(3);
        review.setId(1L);
        when(userRepository.findByEmail("reviewer@test.com")).thenReturn(Optional.of(reviewer));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);
        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(agent));
        when(reviewRepository.calculateTotalReviews(100L)).thenReturn(1L);
        when(reviewRepository.calculateAvgRating(100L)).thenReturn(Optional.of(5.0));

        AgentReviewResponse res = service.update(1L, "reviewer@test.com",
                new UpdateAgentReviewRequest(5, "updated"));

        assertThat(res.rating()).isEqualTo(5);
        assertThat(review.getComment()).isEqualTo("updated");
    }

    @Test
    void update_byNonOwner_throwsForbidden() {
        AgentReview review = AgentReview.builder().agent(agent).build();
        review.setUser(agentUser);
        review.setId(1L);
        when(userRepository.findByEmail("reviewer@test.com")).thenReturn(Optional.of(reviewer));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> service.update(1L, "reviewer@test.com",
                new UpdateAgentReviewRequest(5, "x")))
                .isInstanceOf(ForbiddenException.class);
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_byOwner_ok() {
        AgentReview review = AgentReview.builder().agent(agent).build();
        review.setUser(reviewer);
        review.setId(1L);
        when(userRepository.findByEmail("reviewer@test.com")).thenReturn(Optional.of(reviewer));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(agent));
        when(reviewRepository.calculateTotalReviews(100L)).thenReturn(0L);
        when(reviewRepository.calculateAvgRating(100L)).thenReturn(Optional.empty());

        service.delete(1L, "reviewer@test.com");

        verify(reviewRepository).delete(review);
        verify(agentProfileRepository).save(agent);
    }

    @Test
    void delete_byAdmin_ok() {
        User admin = User.builder().email("admin@test.com").role(UserRole.ADMIN).build();
        admin.setId(99L);
        AgentReview review = AgentReview.builder().agent(agent).build();
        review.setUser(reviewer);
        review.setId(1L);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(agentProfileRepository.findById(100L)).thenReturn(Optional.of(agent));
        when(reviewRepository.calculateTotalReviews(100L)).thenReturn(0L);
        when(reviewRepository.calculateAvgRating(100L)).thenReturn(Optional.empty());

        service.delete(1L, "admin@test.com");

        verify(reviewRepository).delete(review);
    }

    @Test
    void delete_byOther_throwsForbidden() {
        User other = User.builder().email("other@test.com").role(UserRole.USER).build();
        other.setId(77L);
        AgentReview review = AgentReview.builder().agent(agent).build();
        review.setUser(reviewer);
        review.setId(1L);
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(other));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> service.delete(1L, "other@test.com"))
                .isInstanceOf(ForbiddenException.class);
    }

    // ─── getReviews / getSummary ──────────────────────────────────────────────

    @Test
    void getReviews_agentNotFound_throws() {
        when(agentProfileRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> service.getReviews(404L, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getReviews_returnsPage() {
        AgentReview r = AgentReview.builder().agent(agent).build();
        r.setUser(reviewer);
        r.setId(1L);
        r.setRating(4);
        when(agentProfileRepository.existsById(100L)).thenReturn(true);
        when(reviewRepository.findByAgent_Id(eq100L(), any())).thenReturn(new PageImpl<>(List.of(r)));

        Page<AgentReviewResponse> page = service.getReviews(100L, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).rating()).isEqualTo(4);
    }

    @Test
    void getSummary_withReviews_returnsAvgAndCount() {
        when(agentProfileRepository.existsById(100L)).thenReturn(true);
        when(reviewRepository.countByAgent_Id(100L)).thenReturn(3L);
        when(reviewRepository.avgRatingByAgentId(100L)).thenReturn(4.333);

        AgentReviewSummaryResponse res = service.getSummary(100L);

        assertThat(res.reviewCount()).isEqualTo(3L);
        assertThat(res.avgRating()).isEqualByComparingTo("4.33");
    }

    @Test
    void getSummary_noReviews_returnsZero() {
        when(agentProfileRepository.existsById(100L)).thenReturn(true);
        when(reviewRepository.countByAgent_Id(100L)).thenReturn(0L);
        when(reviewRepository.avgRatingByAgentId(100L)).thenReturn(null);

        AgentReviewSummaryResponse res = service.getSummary(100L);

        assertThat(res.reviewCount()).isZero();
        assertThat(res.avgRating()).isEqualByComparingTo("0");
    }

    private Long eq100L() {
        return org.mockito.ArgumentMatchers.eq(100L);
    }
}
