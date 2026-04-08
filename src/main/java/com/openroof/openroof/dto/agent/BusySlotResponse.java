package com.openroof.openroof.dto.agent;

import java.time.LocalDateTime;

/**
 * Represents a busy time slot in an agent's schedule.
 * Used by the availability endpoint so buyers know which hours are occupied.
 */
public record BusySlotResponse(
        LocalDateTime startTime,
        LocalDateTime endTime,
        String reason
) {}
