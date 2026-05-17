package com.openroof.openroof.service;

import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.admin.AuditLog;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.agent.AgentReview;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentReviewRepository;
import com.openroof.openroof.repository.AuditLogRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAgentReviewService")
class AdminAgentReviewServiceTest {

    @Mock private AgentReviewRepository reviewRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AgentReviewService agentReviewService;

    private AdminAgentReviewService service;

    private User admin;
    private AgentProfile agent;
    private User reviewer;

    @BeforeEach
    void setUp() {
        service = new AdminAgentReviewService(reviewRepository, userRepository, auditLogRepository, agentReviewService);

        admin = User.builder().email("admin@test.com").role(UserRole.ADMIN).build();
        admin.setId(1L);

        reviewer = User.builder().email("reviewer@test.com").role(UserRole.USER).build();
        reviewer.setId(10L);

        agent = AgentProfile.builder().build();
        agent.setId(100L);
    }

    @Test
    void deleteReviewAsAdmin_createsAuditLogAndRecalculatesRating() {
        AgentReview review = AgentReview.builder().agent(agent).build();
        review.setUser(reviewer);
        review.setRating(2);
        review.setComment("bad");
        review.setId(500L);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(reviewRepository.findById(500L)).thenReturn(Optional.of(review));

        service.deleteReviewAsAdmin(500L, "admin@test.com", "Contenido ofensivo");

        verify(reviewRepository).delete(review);
        verify(agentReviewService).recalculateAgentRating(agent.getId());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog logged = captor.getValue();
        assertThat(logged.getAction()).isEqualTo("REVIEW_DELETED");
        assertThat(logged.getEntityType()).isEqualTo("AGENT_REVIEW");
        assertThat(logged.getEntityId()).isEqualTo(500L);
        assertThat(logged.getUser()).isEqualTo(admin);
        assertThat(logged.getOldValues()).containsEntry("rating", 2);
        assertThat(logged.getNewValues()).containsEntry("moderationReason", "Contenido ofensivo");
        assertThat(logged.getNewValues()).containsEntry("agentId", 100L);
    }

    @Test
    void deleteReviewAsAdmin_reviewNotFound_throws() {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(reviewRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteReviewAsAdmin(404L, "admin@test.com", "x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
