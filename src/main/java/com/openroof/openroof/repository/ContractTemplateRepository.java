package com.openroof.openroof.repository;

import com.openroof.openroof.model.contract.ContractTemplate;
import com.openroof.openroof.model.enums.ContractType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {

    @Query("""
            SELECT t FROM ContractTemplate t
            WHERE (:contractType IS NULL OR t.contractType = :contractType)
            AND (:active IS NULL OR t.active = :active)
            ORDER BY t.contractType ASC, t.name ASC
            """)
    List<ContractTemplate> findAdminFiltered(
            @Param("contractType") ContractType contractType,
            @Param("active") Boolean active);

    List<ContractTemplate> findByContractTypeAndActiveTrueOrderByNameAsc(ContractType contractType);
}
