package com.openroof.openroof.service;

import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.admin.AuditLog;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.agent.AgentReview;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentReviewRepository;
import com.openroof.openroof.repository.AuditLogRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminAgentReviewService {

    private final AgentReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AgentReviewService agentReviewService;

    @Transactional
    public void deleteReviewAsAdmin(Long reviewId, String adminEmail, String moderationReason) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", adminEmail));

        AgentReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("AgentReview", "id", reviewId));

        AgentProfile agent = review.getAgent();
        Long agentId = agent != null ? agent.getId() : null;

        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("rating", review.getRating());
        oldValues.put("comment", review.getComment());
        oldValues.put("reviewerId", review.getUser() != null ? review.getUser().getId() : null);
        oldValues.put("agentId", agentId);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("agentId", agentId);
        metadata.put("moderationReason", moderationReason);

        reviewRepository.delete(review);

        AuditLog auditLog = AuditLog.builder()
                .user(admin)
                .action("REVIEW_DELETED")
                .entityType("AGENT_REVIEW")
                .entityId(reviewId)
                .oldValues(oldValues)
                .newValues(metadata)
                .build();
        auditLogRepository.save(auditLog);

        if (agent != null) {
            agentReviewService.recalculateAgentRating(agent.getId());
        }

        log.info("Admin {} deleted AgentReview {} (reason: {})", admin.getId(), reviewId, moderationReason);
    }
}
