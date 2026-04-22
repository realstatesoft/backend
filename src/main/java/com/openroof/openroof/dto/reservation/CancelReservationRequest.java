package com.openroof.openroof.dto.reservation;

import jakarta.validation.constraints.Size;

public record CancelReservationRequest(
        @Size(max = 1000) String reason
) {}