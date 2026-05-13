package com.openroof.openroof.dto.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminDeleteAgentReviewRequest(

        @NotBlank @Size(max = 1000) String moderationReason

) {}
