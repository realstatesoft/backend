package com.openroof.openroof.service;

import com.openroof.openroof.dto.agent.AgentRatingSummaryResponse;
import com.openroof.openroof.dto.agent.AgentReviewResponse;
import com.openroof.openroof.dto.agent.CreateAgentReviewRequest;
import com.openroof.openroof.exception.ConflictException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.AgentReviewMapper;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.agent.AgentReview;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AgentReviewService {

    private final AgentReviewRepository reviewRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final AgentReviewMapper reviewMapper;

    @Transactional
    public AgentReviewResponse createReview(Long agentId, Long userId, CreateAgentReviewRequest dto) {
        User reviewer = getUserById(userId);
        AgentProfile agent = agentProfileRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("AgentProfile", "id", agentId));

        if (agent.getUser() != null && agent.getUser().getId().equals(userId)) {
            throw new ConflictException("No podés reseñarte a vos mismo");
        }

        if (reviewRepository.existsByAgent_IdAndUser_Id(agentId, userId)) {
            throw new ConflictException("Ya existe una reseña de este usuario para este agente");
        }

        Property property = null;
        if (dto.propertyId() != null) {
            property = propertyRepository.findById(dto.propertyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Property", "id", dto.propertyId()));
        }

        AgentReview review = reviewMapper.toEntity(dto, agent, reviewer, property);

        AgentReview saved;
        try {
            saved = reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Ya existe una reseña de este usuario para este agente");
        }

        recalculateAgentRating(agentId);
        log.info("AgentReview {} created for agent={} by reviewer={}", saved.getId(), agentId, reviewer.getId());
        return toResponse(saved, reviewer.getId());
    }

    @Transactional
    public AgentReviewResponse updateReview(Long reviewId, Long userId, CreateAgentReviewRequest dto) {
        AgentReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("AgentReview", "id", reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Solo el autor puede modificar esta reseña");
        }

        review.setRating(dto.rating());
        review.setComment(dto.comment());

        AgentReview saved = reviewRepository.save(review);
        recalculateAgentRating(saved.getAgent().getId());
        return toResponse(saved);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        AgentReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("AgentReview", "id", reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Solo el autor puede eliminar esta reseña");
        }

        AgentProfile agent = review.getAgent();
        reviewRepository.delete(review);
        recalculateAgentRating(agent.getId());
    }

    public Page<AgentReviewResponse> getReviews(Long agentId, Long currentUserId, Pageable pageable) {
        if (!agentProfileRepository.existsById(agentId)) {
            throw new ResourceNotFoundException("AgentProfile", "id", agentId);
        }
        return reviewRepository.findByAgent_Id(agentId, pageable)
                .map(r -> reviewMapper.toResponse(r, currentUserId));
    }

    public AgentRatingSummaryResponse getRatingSummary(Long agentId, Long currentUserId) {
        AgentProfile agent = agentProfileRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("AgentProfile", "id", agentId));

        Map<Integer, Long> distribution = reviewRepository
                .countRatingDistributionByAgentId(agentId)
                .stream()
                .collect(Collectors.toMap(
                        AgentReviewRepository.RatingDistribution::getRating,
                        AgentReviewRepository.RatingDistribution::getCount));

        var latestReviews = reviewRepository.findTop5ByAgent_IdOrderByCreatedAtDesc(agentId)
                .stream()
                .map(r -> reviewMapper.toResponse(r, currentUserId))
                .toList();

        return reviewMapper.toSummaryResponse(agent, latestReviews, distribution);
    }

    public AgentRatingSummaryResponse getRatingSummary(Long agentId) {
        if (!agentProfileRepository.existsById(agentId)) {
            throw new ResourceNotFoundException("AgentProfile", "id", agentId);
        }
        long count = reviewRepository.countByAgent_Id(agentId);
        Double avg = reviewRepository.avgRatingByAgentId(agentId);
        BigDecimal avgRating = avg == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
        Map<Integer, Long> distribution = reviewRepository.countRatingDistributionByAgentId(agentId)
                .stream()
                .collect(Collectors.toMap(
                        AgentReviewRepository.RatingDistribution::getRating,
                        AgentReviewRepository.RatingDistribution::getCount));
        List<AgentReviewResponse> latestReviews = reviewRepository
                .findTop5ByAgent_IdOrderByCreatedAtDesc(agentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new AgentRatingSummaryResponse(avgRating, (int) count, distribution, latestReviews);
    }

    public Optional<AgentReviewResponse> getMyReview(Long agentId, String userEmail) {
        if (!agentProfileRepository.existsById(agentId)) {
            throw new ResourceNotFoundException("AgentProfile", "id", agentId);
        }
        User user = getUser(userEmail);
        return reviewRepository.findByAgent_IdAndUser_Id(agentId, user.getId())
                .map(r -> toResponse(r, true));
    }

    void recalculateAgentRating(AgentProfile agent) {
        long count = reviewRepository.countByAgent_Id(agent.getId());
        Double avg = reviewRepository.avgRatingByAgentId(agent.getId());
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
        return toResponse(r, false);
    }

    private AgentReviewResponse toResponse(AgentReview r, boolean isOwn) {
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
                isOwn);
    }
}
