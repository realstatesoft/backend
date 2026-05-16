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
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.AgentReviewRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AgentReviewService {

    private final AgentReviewRepository reviewRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;

    @Transactional
    public AgentReviewResponse create(Long agentId, String reviewerEmail, CreateAgentReviewRequest req) {
        User reviewer = getUser(reviewerEmail);
        AgentProfile agent = agentProfileRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("AgentProfile", "id", agentId));

        if (agent.getUser() != null && agent.getUser().getId().equals(reviewer.getId())) {
            throw new BadRequestException("No te podés reseñar a vos mismo");
        }

        if (reviewRepository.existsByAgent_IdAndUser_Id(agentId, reviewer.getId())) {
            throw new ConflictException("Ya reseñaste a este agente");
        }

        Property property = null;
        if (req.propertyId() != null) {
            property = propertyRepository.findById(req.propertyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Property", "id", req.propertyId()));
        }

        AgentReview review = AgentReview.builder()
                .agent(agent)
                .property(property)
                .build();
        review.setUser(reviewer);
        review.setRating(req.rating());
        review.setComment(req.comment());

        AgentReview saved;
        try {
            saved = reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Ya reseñaste a este agente");
        }

        recalculateAgentRating(agentId);
        log.info("AgentReview {} created for agent={} by reviewer={}", saved.getId(), agentId, reviewer.getId());
        return toResponse(saved);
    }

    @Transactional
    public AgentReviewResponse update(Long reviewId, String userEmail, UpdateAgentReviewRequest req) {
        User user = getUser(userEmail);
        AgentReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("AgentReview", "id", reviewId));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Solo el autor puede modificar esta reseña");
        }

        if (req.rating() != null) review.setRating(req.rating());
        if (req.comment() != null) review.setComment(req.comment());

        AgentReview saved = reviewRepository.save(review);
        recalculateAgentRating(saved.getAgent().getId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long reviewId, String userEmail) {
        User user = getUser(userEmail);
        AgentReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("AgentReview", "id", reviewId));

        boolean isOwner = review.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == UserRole.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Solo el autor o un ADMIN pueden eliminar esta reseña");
        }

        AgentProfile agent = review.getAgent();
        reviewRepository.delete(review);
        recalculateAgentRating(agent.getId());
    }

    public Page<AgentReviewResponse> getReviews(Long agentId, Pageable pageable) {
        if (!agentProfileRepository.existsById(agentId)) {
            throw new ResourceNotFoundException("AgentProfile", "id", agentId);
        }
        return reviewRepository.findByAgent_Id(agentId, pageable).map(this::toResponse);
    }

    public AgentReviewSummaryResponse getSummary(Long agentId) {
        if (!agentProfileRepository.existsById(agentId)) {
            throw new ResourceNotFoundException("AgentProfile", "id", agentId);
        }
        long count = reviewRepository.countByAgent_Id(agentId);
        Double avg = reviewRepository.avgRatingByAgentId(agentId);
        BigDecimal avgRating = avg == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
        return new AgentReviewSummaryResponse(agentId, avgRating, count);
    }

    private void recalculateAgentRating(Long agentId) {
        AgentProfile agent = agentProfileRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("AgentProfile", "id", agentId));
        long count = reviewRepository.calculateTotalReviews(agentId);
        Double avg = reviewRepository.calculateAvgRating(agentId).orElse(null);
        agent.setTotalReviews((int) count);
        agent.setAvgRating(avg == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        agentProfileRepository.save(agent);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private AgentReviewResponse toResponse(AgentReview r) {
        return new AgentReviewResponse(
                r.getId(),
                r.getAgent() != null ? r.getAgent().getId() : null,
                r.getUser() != null ? r.getUser().getId() : null,
                r.getUser() != null ? r.getUser().getName() : null,
                r.getUser() != null ? r.getUser().getAvatarUrl() : null,
                r.getProperty() != null ? r.getProperty().getId() : null,
                r.getProperty() != null ? r.getProperty().getAddress() : null,
                r.getRating(),
                r.getComment(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                false);
    }
}
