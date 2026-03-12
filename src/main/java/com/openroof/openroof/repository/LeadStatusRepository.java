package com.openroof.openroof.repository;

import com.openroof.openroof.model.lead.LeadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeadStatusRepository extends JpaRepository<LeadStatus, Long> {

    Optional<LeadStatus> findByName(String name);

    Optional<LeadStatus> findFirstByActiveTrue();

    /**
     * Busca un LeadStatus por nombre incluyendo los registros soft-deleted.
     * Útil para evitar violaciones de unicidad al restaurar un estado previo.
     */
    @Query(value = "SELECT * FROM lead_statuses WHERE name = :name LIMIT 1", nativeQuery = true)
    Optional<LeadStatus> findByNameIncludingDeleted(@Param("name") String name);
}
