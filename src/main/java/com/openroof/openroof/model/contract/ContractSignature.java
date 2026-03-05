package com.openroof.openroof.model.contract;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.SignatureRole;
import com.openroof.openroof.model.enums.SignatureType;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "contract_signatures", indexes = {
        @Index(name = "idx_signatures_contract", columnList = "contract_id"),
        @Index(name = "idx_signatures_signer", columnList = "signer_id"),
        @Index(name = "idx_signatures_signed", columnList = "signed_at")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractSignature extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signer_id", nullable = false)
    private User signer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SignatureRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "signature_type", nullable = false, length = 50)
    private SignatureType signatureType;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "signature_data", columnDefinition = "TEXT")
    private String signatureData;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    public boolean isSigned() {
        return signedAt != null;
    }
}
