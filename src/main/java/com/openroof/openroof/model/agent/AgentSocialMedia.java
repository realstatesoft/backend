package com.openroof.openroof.model.agent;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.SocialMediaPlatform;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agent_social_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentSocialMedia extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentProfile agent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SocialMediaPlatform platform;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;
}
