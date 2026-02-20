package com.openroof.openroof.model.agent;

import com.openroof.openroof.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agent_specialties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentSpecialty extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;
}
