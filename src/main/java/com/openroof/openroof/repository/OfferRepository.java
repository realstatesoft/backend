package com.openroof.openroof.repository;

import com.openroof.openroof.model.interaction.Offer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"property", "buyer"})
    Page<Offer> findByBuyer_Id(Long buyerId, Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"property", "buyer"})
    Page<Offer> findAll(Pageable pageable);

    @Query("SELECT o FROM Offer o WHERE o.property.owner.id = :ownerId ORDER BY o.createdAt DESC")
    List<Offer> findByPropertyOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT COUNT(o) FROM Offer o WHERE o.property.owner.id = :ownerId")
    long countByPropertyOwnerId(@Param("ownerId") Long ownerId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"property", "buyer"})
    List<Offer> findByProperty_Id(Long propertyId);

    List<Offer> findByProperty_Agent_Id(Long agentId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"property", "buyer"})
    @Query("SELECT o FROM Offer o WHERE o.property.owner.id = :ownerId OR (o.property.agent IS NOT NULL AND o.property.agent.id = :agentId) ORDER BY o.createdAt DESC")
    Page<Offer> findReceivedOffers(@Param("ownerId") Long ownerId, @Param("agentId") Long agentId, Pageable pageable);
}
