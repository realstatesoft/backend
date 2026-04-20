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
    
    Optional<AgentProfile> findByUser_Email(String email);

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

    // keyword  → service passes '%kw%' or '%' (never null)
    // specialty → service passes actual name or '' (never null)
    // minRating → service passes actual value or -1 (never null)
    @Query(value =
           "SELECT a FROM AgentProfile a JOIN FETCH a.user u " +
           "WHERE (LOWER(u.name) LIKE LOWER(:keyword) " +
           "   OR LOWER(a.companyName) LIKE LOWER(:keyword) " +
           "   OR LOWER(a.licenseNumber) LIKE LOWER(:keyword)) " +
           "AND (:specialty = '' OR a.id IN (" +
           "  SELECT a2.id FROM AgentProfile a2 JOIN a2.specialties s2 " +
           "  WHERE LOWER(s2.name) = LOWER(:specialty))) " +
           "AND (:minRating < 0 OR a.avgRating >= :minRating)",
           countQuery =
           "SELECT COUNT(a) FROM AgentProfile a JOIN a.user u " +
           "WHERE (LOWER(u.name) LIKE LOWER(:keyword) " +
           "   OR LOWER(a.companyName) LIKE LOWER(:keyword) " +
           "   OR LOWER(a.licenseNumber) LIKE LOWER(:keyword)) " +
           "AND (:specialty = '' OR a.id IN (" +
           "  SELECT a2.id FROM AgentProfile a2 JOIN a2.specialties s2 " +
           "  WHERE LOWER(s2.name) = LOWER(:specialty))) " +
           "AND (:minRating < 0 OR a.avgRating >= :minRating)")
    Page<AgentProfile> searchWithFilters(
            @Param("keyword")   String keyword,
            @Param("specialty") String specialty,
            @Param("minRating") java.math.BigDecimal minRating,
            Pageable pageable);

    /**
     * Busca agentes que tengan especialidades cuyos nombres coincidan con alguno de los valores dados.
     * Ordena por rating descendente, luego por experiencia descendente (nulls al final).
     * Acepta {@link Pageable} para aplicar el límite en base de datos.
     * <p>
     * Usa sub-select con DISTINCT para evitar el error de PostgreSQL:
     * "for SELECT DISTINCT, ORDER BY expressions must appear in select list".
     */
    @Query("SELECT a FROM AgentProfile a " +
           "JOIN FETCH a.user u " +
           "LEFT JOIN FETCH a.specialties " +
           "WHERE a.id IN (" +
           "  SELECT DISTINCT a2.id FROM AgentProfile a2 " +
           "  JOIN a2.specialties s " +
           "  WHERE LOWER(s.name) IN :specialtyNames" +
           ") " +
           "ORDER BY a.avgRating DESC, " +
           "CASE WHEN a.experienceYears IS NULL THEN 1 ELSE 0 END ASC, " +
           "a.experienceYears DESC")
    List<AgentProfile> findBySpecialtyNamesOrderByRating(
            @Param("specialtyNames") List<String> specialtyNames,
            Pageable pageable);

    @Query("SELECT a FROM AgentProfile a " +
           "JOIN FETCH a.user u " +
           "ORDER BY a.avgRating DESC, CASE WHEN a.experienceYears IS NULL THEN 1 ELSE 0 END ASC, a.experienceYears DESC")
    List<AgentProfile> findTopAgentsOrderByRating(Pageable pageable);

    @Query("SELECT ap FROM AgentProfile ap JOIN FETCH ap.user WHERE ap.id = :id")
    Optional<AgentProfile> findByIdWithUser(@Param("id") Long id);
}
