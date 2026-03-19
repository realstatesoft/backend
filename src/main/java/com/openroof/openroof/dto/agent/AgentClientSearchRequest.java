package com.openroof.openroof.dto.agent;

import com.openroof.openroof.model.enums.ClientStatus;
import com.openroof.openroof.model.enums.ClientType;
import java.time.LocalDateTime;

public record AgentClientSearchRequest(
    String q,
    ClientStatus status,
    ClientType clientType,
    LocalDateTime createdAtFrom,
    LocalDateTime createdAtTo
) {
}
