package com.openroof.openroof.repository;

import com.openroof.openroof.model.contract.ContractSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractSignatureRepository extends JpaRepository<ContractSignature, Long> {

    List<ContractSignature> findByContract_Id(Long contractId);

    Optional<ContractSignature> findByContract_IdAndSigner_Id(Long contractId, Long signerId);

    long countByContract_IdAndSignedAtIsNotNull(Long contractId);

    long countByContract_Id(Long contractId);
}
