package com.openroof.openroof.dto.message;

import java.time.LocalDateTime;

public record ConversationResponse(
    Long id,
    String contactName,
    String lastMessage,
    boolean lastMessageOwn,
    LocalDateTime timestamp,
    int unread,
    String avatar
) {}
