package com.openroof.openroof.dto.agent;
import com.openroof.openroof.model.enums.ClientStatus;
import com.openroof.openroof.model.enums.ClientType;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public record AgentClientSearchRequest(
    String q,
    ClientStatus status,
    ClientType clientType,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate createdAtFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate createdAtTo
) {
}
