package com.openroof.openroof.model.contract;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.ContractType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contract_templates", indexes = {
        @Index(name = "idx_contract_templates_type", columnList = "contract_type"),
        @Index(name = "idx_contract_templates_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractTemplate extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 50)
    private ContractType contractType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "template_version", length = 20)
    @Builder.Default
    private String templateVersion = "1.0";

    @Builder.Default
    private Boolean active = true;
}
