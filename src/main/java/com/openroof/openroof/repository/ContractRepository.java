package com.openroof.openroof.repository;

import com.openroof.openroof.model.contract.Contract;
import com.openroof.openroof.model.enums.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Contract c WHERE c.id = :id")
    Optional<Contract> findByIdForUpdate(@Param("id") Long id);

    List<Contract> findByBuyer_Id(Long buyerId);

    List<Contract> findBySeller_Id(Long sellerId);

    List<Contract> findByStatus(ContractStatus status);

    List<Contract> findByProperty_Id(Long propertyId);

    List<Contract> findByListingAgent_Id(Long listingAgentId);

    List<Contract> findByBuyerAgent_Id(Long buyerAgentId);

    @Query("""
            SELECT c FROM Contract c JOIN c.property p
            WHERE LOWER(COALESCE(p.title, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR c.id = :idOrNegOne
            ORDER BY c.id DESC
            """)
    Page<Contract> searchForAuditPicker(@Param("q") String q, @Param("idOrNegOne") long idOrNegOne, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Contract c WHERE c.seller.id = :sellerId AND c.status = :status")
    long countBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") ContractStatus status);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Contract c WHERE c.seller.id = :sellerId AND c.status = 'SIGNED'")
    BigDecimal sumAmountBySellerIdSigned(@Param("sellerId") Long sellerId);

    @Query("SELECT c FROM Contract c WHERE c.listingAgent.id = :agentId AND c.status = :status")
    List<Contract> findByListingAgentIdAndStatus(@Param("agentId") Long agentId, @Param("status") ContractStatus status);

    @Query("SELECT c FROM Contract c WHERE c.buyerAgent.id = :agentId AND c.status = :status")
    List<Contract> findByBuyerAgentIdAndStatus(@Param("agentId") Long agentId, @Param("status") ContractStatus status);

    @Query("SELECT COUNT(c) FROM Contract c WHERE c.status = 'SIGNED' AND c.startDate IS NOT NULL AND YEAR(c.startDate) = :year AND MONTH(c.startDate) = :month")
    long countSignedByYearAndMonth(@Param("year") int year, @Param("month") int month);

    @Query("""
          SELECT DISTINCT c FROM Contract c
          LEFT JOIN c.listingAgent la
          LEFT JOIN c.buyerAgent ba
          WHERE c.seller.id = :userId
              OR c.buyer.id = :userId
              OR la.id = :agentProfileId
              OR ba.id = :agentProfileId
    """)
    List<Contract> findAllByParticipant(@Param("userId") Long userId,
                                        @Param("agentProfileId") Long agentProfileId);

    @Query("""
        SELECT new com.openroof.openroof.dto.dashboard.RawSalesData(
            YEAR(c.startDate),
            MONTH(c.startDate),
            COALESCE(SUM(c.amount), 0),
            COUNT(c)
        )
        FROM Contract c
        WHERE c.seller.id = :sellerId
          AND c.startDate IS NOT NULL
          AND c.property.status IN :statuses
          AND YEAR(c.startDate) IN (:currentYear, :previousYear)
        GROUP BY YEAR(c.startDate), MONTH(c.startDate)
        ORDER BY YEAR(c.startDate), MONTH(c.startDate)
    """)
    List<com.openroof.openroof.dto.dashboard.RawSalesData> findMonthlySalesGrouped(
        @Param("sellerId") Long sellerId,
        @Param("statuses") List<com.openroof.openroof.model.enums.PropertyStatus> statuses,
        @Param("currentYear") int currentYear,
        @Param("previousYear") int previousYear
    );

    @Query("""
        SELECT c FROM Contract c
        WHERE (c.seller.id = :userId OR c.buyer.id = :userId)
          AND c.status IN (
            com.openroof.openroof.model.enums.ContractStatus.SENT,
            com.openroof.openroof.model.enums.ContractStatus.PARTIALLY_SIGNED
          )
          AND NOT EXISTS (
              SELECT 1 FROM ContractSignature cs
              WHERE cs.contract.id = c.id
                AND cs.signer.id = :userId
                AND cs.deletedAt IS NULL
          )
    """)
    List<Contract> findPendingSignaturesForUser(@Param("userId") Long userId);
}
