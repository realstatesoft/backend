package com.openroof.openroof.model.messaging;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_messages_sender", columnList = "sender_id"),
        @Index(name = "idx_messages_receiver", columnList = "receiver_id"),
        @Index(name = "idx_messages_property", columnList = "property_id"),
        @Index(name = "idx_messages_parent", columnList = "parent_id"),
        @Index(name = "idx_messages_created", columnList = "created_at")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Message parent;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
