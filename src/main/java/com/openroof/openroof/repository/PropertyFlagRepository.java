package com.openroof.openroof.repository;

import com.openroof.openroof.model.admin.PropertyFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyFlagRepository extends JpaRepository<PropertyFlag, Long> {

    /** Flags activos (no resueltos) de una propiedad concreta. */
    List<PropertyFlag> findByPropertyIdAndResolvedAtIsNull(Long propertyId);

    /** Cantidad de flags activos de una propiedad (para el contador público). */
    long countByPropertyIdAndResolvedAtIsNull(Long propertyId);

    /** Todos los flags activos en el sistema (uso exclusivo ADMIN). */
    List<PropertyFlag> findAllByResolvedAtIsNull();

    /**
     * Anti-spam: verifica si el usuario ya tiene un flag activo para esa propiedad.
     * Si hay resultado, se rechaza la creación del nuevo flag.
     */
    Optional<PropertyFlag> findByPropertyIdAndReportedByIdAndResolvedAtIsNull(Long propertyId, Long reportedById);
}
