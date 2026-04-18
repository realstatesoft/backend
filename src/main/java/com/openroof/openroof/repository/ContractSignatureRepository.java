package com.openroof.openroof.repository;

import com.openroof.openroof.model.contract.ContractSignature;
import com.openroof.openroof.model.enums.SignatureRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractSignatureRepository extends JpaRepository<ContractSignature, Long> {

    List<ContractSignature> findByContractIdAndDeletedAtIsNull(Long contractId);

    boolean existsByContractIdAndRoleAndDeletedAtIsNull(Long contractId, SignatureRole role);

    Optional<ContractSignature> findByContractIdAndSignerIdAndDeletedAtIsNull(Long contractId, Long signerId);

    long countByContractIdAndSignedAtIsNotNullAndDeletedAtIsNull(Long contractId);
}
