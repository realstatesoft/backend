package com.openroof.openroof.repository;

import com.openroof.openroof.dto.dashboard.RawSalesData;
import com.openroof.openroof.model.contract.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    @Query("SELECT new com.openroof.openroof.dto.dashboard.RawSalesData(" +
            "YEAR(c.startDate), MONTH(c.startDate), SUM(c.amount), COUNT(c.id)) " +
            "FROM Contract c " +
            "JOIN c.property p " +
            "WHERE p.agent.user.email = :agentEmail " +
            "AND p.status IN (com.openroof.openroof.model.enums.PropertyStatus.SOLD, " +
            "                 com.openroof.openroof.model.enums.PropertyStatus.RENTED) " +
            "AND YEAR(c.startDate) IN (:currentYear, :previousYear) " +
            "GROUP BY YEAR(c.startDate), MONTH(c.startDate)")
    List<RawSalesData> getAgentPerformanceData(
            @Param("agentEmail") String agentEmail,
            @Param("currentYear") int currentYear,
            @Param("previousYear") int previousYear
    );
}
