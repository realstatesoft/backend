package com.openroof.openroof.dto.message;

import java.time.LocalDateTime;

public record MessageResponse(
    Long id,
    String text,
    String sender,
    LocalDateTime timestamp,
    Long senderId,
    Long receiverId
) {}
