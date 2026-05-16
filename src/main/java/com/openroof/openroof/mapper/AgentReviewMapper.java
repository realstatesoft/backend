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
        User reviewer = review.getUser();
        Property property = review.getProperty();
        AgentProfile agent = review.getAgent();

        boolean isOwn = currentUserId != null
                && reviewer != null
                && currentUserId.equals(reviewer.getId());

        return new AgentReviewResponse(
                review.getId(),
                agent != null ? agent.getId() : null,
                reviewer != null ? reviewer.getId() : null,
                reviewer != null ? reviewer.getName() : null,
                reviewer != null ? reviewer.getAvatarUrl() : null,
                property != null ? property.getId() : null,
                property != null ? property.getAddress() : null,
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                isOwn
        );
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

    public AgentRatingSummaryResponse toSummaryResponse(
            AgentProfile agent,
            List<AgentReviewResponse> latestReviews,
            Map<Integer, Long> ratingDistribution) {
        return new AgentRatingSummaryResponse(
                agent.getAvgRating(),
                agent.getTotalReviews(),
                ratingDistribution,
                latestReviews
        );
    }
}
