package com.openroof.openroof.dto.message;

import java.time.LocalDateTime;

public record MessageResponse(
    Long id,
    String text,
    boolean ownMessage,
    String senderName,
    String senderRole,
    LocalDateTime timestamp,
    Long senderId,
    Long receiverId
) {}
