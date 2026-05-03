package com.openroof.openroof.model.config;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.NotifyChannel;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "user_settings", indexes = {
        @Index(name = "idx_user_settings_user", columnList = "user_id", unique = true)
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "notify_price_drop", nullable = false)
    @Builder.Default
    private boolean notifyPriceDrop = true;

    @Column(name = "notify_new_match", nullable = false)
    @Builder.Default
    private boolean notifyNewMatch = true;

    @Column(name = "notify_messages", nullable = false)
    @Builder.Default
    private boolean notifyMessages = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "notify_channel", nullable = false, length = 10)
    @Builder.Default
    private NotifyChannel notifyChannel = NotifyChannel.BOTH;

    @Column(name = "profile_visible_to_agents", nullable = false)
    @Builder.Default
    private boolean profileVisibleToAgents = true;

    @Column(name = "allow_direct_contact", nullable = false)
    @Builder.Default
    private boolean allowDirectContact = true;
}
