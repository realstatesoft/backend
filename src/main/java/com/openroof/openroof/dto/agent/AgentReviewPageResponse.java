package com.openroof.openroof.dto.agent;

import org.springframework.data.domain.Page;

import java.util.List;

public record AgentReviewPageResponse(
        List<AgentReviewResponse> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean hasNextPage
) {
    public static AgentReviewPageResponse from(Page<AgentReviewResponse> page) {
        return new AgentReviewPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
