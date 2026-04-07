package com.openroof.openroof.repository;

import com.openroof.openroof.model.contract.ContractTemplate;
import com.openroof.openroof.model.enums.ContractType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {

    List<ContractTemplate> findByContractTypeAndActiveTrue(ContractType contractType);

    List<ContractTemplate> findByActiveTrue();
}
