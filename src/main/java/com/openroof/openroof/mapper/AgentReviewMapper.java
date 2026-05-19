package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.agent.AgentRatingSummaryResponse;
import com.openroof.openroof.dto.agent.AgentReviewResponse;
import com.openroof.openroof.dto.agent.CreateAgentReviewRequest;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.agent.AgentReview;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AgentReviewMapper {

    public AgentReviewResponse toResponse(AgentReview review, Long currentUserId) {
        boolean isOwn = currentUserId != null
                && review.getUser() != null
                && currentUserId.equals(review.getUser().getId());

        return new AgentReviewResponse(
                review.getId(),
                review.getAgent() != null ? review.getAgent().getId() : null,
                review.getUser() != null ? review.getUser().getId() : null,
                review.getUser() != null ? review.getUser().getName() : null,
                review.getUser() != null ? review.getUser().getAvatarUrl() : null,
                review.getProperty() != null ? review.getProperty().getId() : null,
                review.getProperty() != null ? review.getProperty().getAddress() : null,
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                isOwn);
    }

    public AgentReview toEntity(CreateAgentReviewRequest dto, AgentProfile agent, User user, Property property) {
        AgentReview review = AgentReview.builder()
                .agent(agent)
                .property(property)
                .build();
        review.setUser(user);
        review.setRating(dto.rating());
        review.setComment(dto.comment());
        return review;
    }

    public AgentRatingSummaryResponse toSummaryResponse(AgentProfile agent,
                                                         List<AgentReviewResponse> latestReviews,
                                                         Map<Integer, Long> distribution) {
        return new AgentRatingSummaryResponse(
                agent.getAvgRating(),
                agent.getTotalReviews(),
                distribution,
                latestReviews);
    }
}
