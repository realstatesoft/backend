package com.openroof.openroof.repository;

import com.openroof.openroof.model.interaction.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findByBuyer_Id(Long buyerId);

    @Query("SELECT o FROM Offer o WHERE o.property.owner.id = :ownerId ORDER BY o.createdAt DESC")
    List<Offer> findByPropertyOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT COUNT(o) FROM Offer o WHERE o.property.owner.id = :ownerId")
    long countByPropertyOwnerId(@Param("ownerId") Long ownerId);
}
