package com.openroof.openroof.repository;

import com.openroof.openroof.model.agent.AgentProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentProfileRepository extends JpaRepository<AgentProfile, Long> {

    Optional<AgentProfile> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);

    @Query(value = "SELECT a FROM AgentProfile a JOIN FETCH a.user",
           countQuery = "SELECT COUNT(a) FROM AgentProfile a")
    Page<AgentProfile> findAllWithUser(Pageable pageable);

    @Query(value = "SELECT a FROM AgentProfile a JOIN FETCH a.user u " +
           "WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.licenseNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))",
           countQuery = "SELECT COUNT(a) FROM AgentProfile a JOIN a.user u " +
           "WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.licenseNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<AgentProfile> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Busca agentes que tengan especialidades cuyos nombres coincidan con alguno de los valores dados.
     * Ordena por rating descendente, luego por experiencia descendente (nulls al final).
     * Acepta {@link Pageable} para aplicar el límite en base de datos.
     */
    @Query("SELECT DISTINCT a FROM AgentProfile a " +
           "JOIN FETCH a.user u " +
           "LEFT JOIN a.specialties s " +
           "WHERE LOWER(s.name) IN :specialtyNames " +
           "ORDER BY a.avgRating DESC, CASE WHEN a.experienceYears IS NULL THEN 1 ELSE 0 END ASC, a.experienceYears DESC")
    List<AgentProfile> findBySpecialtyNamesOrderByRating(
            @Param("specialtyNames") List<String> specialtyNames,
            Pageable pageable);

    /**
     * Obtiene los agentes mejor calificados (top N por rating y experiencia, nulls al final).
     */
    @Query("SELECT a FROM AgentProfile a " +
           "JOIN FETCH a.user u " +
           "ORDER BY a.avgRating DESC, CASE WHEN a.experienceYears IS NULL THEN 1 ELSE 0 END ASC, a.experienceYears DESC")
    List<AgentProfile> findTopAgentsOrderByRating(Pageable pageable);
}
