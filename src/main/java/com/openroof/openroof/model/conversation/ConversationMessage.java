package com.openroof.openroof.model.conversation;

import com.openroof.openroof.model.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// No soft delete — messages are not removed, just conversations are archived
@Entity
@Table(name = "conversation_messages", indexes = {
        @Index(name = "idx_conversation_messages_conv_sent", columnList = "conversation_id, sent_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> attachments;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "read_by", columnDefinition = "jsonb")
    private Map<String, Object> readBy;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    public boolean isReadBy(Long userId) {
        return readBy != null && readBy.containsKey(String.valueOf(userId));
    }
}
