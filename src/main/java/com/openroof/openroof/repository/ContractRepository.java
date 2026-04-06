package com.openroof.openroof.repository;

import com.openroof.openroof.model.contract.Contract;
import com.openroof.openroof.model.enums.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findByBuyer_Id(Long buyerId);

    List<Contract> findBySeller_Id(Long sellerId);

    List<Contract> findByStatus(ContractStatus status);

    List<Contract> findByProperty_Id(Long propertyId);

    List<Contract> findByListingAgent_Id(Long listingAgentId);

    List<Contract> findByBuyerAgent_Id(Long buyerAgentId);

    @Query("SELECT COUNT(c) FROM Contract c WHERE c.seller.id = :sellerId AND c.status = :status")
    long countBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") ContractStatus status);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Contract c WHERE c.seller.id = :sellerId AND c.status = 'SIGNED'")
    BigDecimal sumAmountBySellerIdSigned(@Param("sellerId") Long sellerId);

    @Query("SELECT c FROM Contract c WHERE c.listingAgent.id = :agentId AND c.status = :status")
    List<Contract> findByListingAgentIdAndStatus(@Param("agentId") Long agentId, @Param("status") ContractStatus status);

    @Query("SELECT c FROM Contract c WHERE c.buyerAgent.id = :agentId AND c.status = :status")
    List<Contract> findByBuyerAgentIdAndStatus(@Param("agentId") Long agentId, @Param("status") ContractStatus status);

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
}
