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
    
    boolean existsByContractIdAndSignerIdAndDeletedAtIsNull(Long contractId, Long signerId);

    @org.springframework.data.jpa.repository.Query("SELECT s.contract.id FROM ContractSignature s WHERE s.contract.id IN :contractIds AND s.signer.id = :signerId AND s.deletedAt IS NULL")
    java.util.Set<Long> findSignedContractIds(@org.springframework.data.repository.query.Param("contractIds") java.util.Collection<Long> contractIds, @org.springframework.data.repository.query.Param("signerId") Long signerId);

    long countByContractIdAndSignedAtIsNotNullAndDeletedAtIsNull(Long contractId);
}
