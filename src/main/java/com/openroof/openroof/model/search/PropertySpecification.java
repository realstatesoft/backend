package com.openroof.openroof.model.search;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.openroof.openroof.dto.property.PropertyFilterRequest;
import com.openroof.openroof.model.enums.Availability;
import com.openroof.openroof.model.enums.PropertyCategory;
import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.property.Property;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Construye un {@link Specification} de JPA dinámicamente a partir de
 * un {@link PropertyFilterRequest}. Solo se agregan predicados para los
 * campos no nulos, y todos se combinan con AND.
 *
 * IMPORTANTE: el JOIN con location solo se hace en la data query (no en
 * la count query) para evitar el error clásico de "query specified join
 * fetching, but the owner of the fetched association was not present in
 * the select list" que produce 500 en endpoints paginados.
 */
public final class PropertySpecification {

    private PropertySpecification() {
    }

    public static Specification<Property> buildFilter(PropertyFilterRequest filter) {
        return (Root<Property> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return cb.conjunction();
            }

            // ── Distinct para evitar duplicados por joins ──────────────────
            // Solo aplica en la data query (Long.class es la count query)
            boolean isCountQuery = query.getResultType() == Long.class;
            if (!isCountQuery) {
                query.distinct(true);
            }

            // ── Excluir propiedades en papelera ──────────────────────────
            predicates.add(cb.isNull(root.get("trashedAt")));

            // ── Búsqueda Textual (keyword) ────────────────────────────────
            if (filter.q() != null && !filter.q().isBlank()) {
                String pattern = "%" + filter.q().trim().toLowerCase() + "%";
                List<Predicate> orPredicates = new ArrayList<>();

                orPredicates.add(cb.like(cb.lower(root.get("title")), pattern));
                orPredicates.add(cb.like(cb.lower(root.get("description")), pattern));
                orPredicates.add(cb.like(cb.lower(root.get("address")), pattern));

                // Búsqueda en nombre de location
                if (isCountQuery) {
                    // En count query usamos subquery para el name
                    Subquery<Long> subquery = query.subquery(Long.class);
                    Root<Property> subRoot = subquery.from(Property.class);
                    subquery.select(cb.literal(1L))
                        .where(
                            cb.equal(subRoot.get("id"), root.get("id")),
                            cb.or(
                                cb.like(cb.lower(subRoot.get("location").get("name")), pattern),
                                cb.like(cb.lower(subRoot.get("location").get("city")), pattern),
                                cb.like(cb.lower(subRoot.get("location").get("department")), pattern)
                            )
                        );
                    orPredicates.add(cb.exists(subquery));
                } else {
                    Join<Object, Object> locationJoinForSearch = root.join("location", JoinType.LEFT);
                    orPredicates.add(cb.like(cb.lower(locationJoinForSearch.get("name")), pattern));
                    orPredicates.add(cb.like(cb.lower(locationJoinForSearch.get("city")), pattern));
                    orPredicates.add(cb.like(cb.lower(locationJoinForSearch.get("department")), pattern));
                }

                predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
            }

            // ── Disponibilidad ────────────────────────────────────────────
            if (filter.availability() != null && !filter.availability().isBlank()) {
                try {
                    Availability avail = Availability.valueOf(filter.availability().trim().toUpperCase());
                    predicates.add(cb.equal(root.get("availability"), avail));
                } catch (IllegalArgumentException ignored) {
                    // Valor inválido → ignorar filtro en lugar de reventar con 500
                }
            }

            // ── Tipo de propiedad ─────────────────────────────────────────
            if (filter.propertyType() != null && !filter.propertyType().isBlank()) {
                try {
                    PropertyType type = PropertyType.valueOf(filter.propertyType().trim().toUpperCase());
                    predicates.add(cb.equal(root.get("propertyType"), type));
                } catch (IllegalArgumentException ignored) {
                }
            }

            // ── Categoría (Operación) ─────────────────────────────────────
            if (filter.category() != null && !filter.category().isBlank()) {
                try {
                    PropertyCategory category = PropertyCategory.valueOf(filter.category().trim().toUpperCase());
                    predicates.add(cb.equal(root.get("category"), category));
                } catch (IllegalArgumentException ignored) {
                }
            }

            // ── Estado ────────────────────────────────────────────────────
            if (filter.status() != null && !filter.status().isBlank()) {
                try {
                    PropertyStatus status = PropertyStatus.valueOf(filter.status().trim().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException ignored) {
                }
            }

            // ── Rango de precio ───────────────────────────────────────────
            if (filter.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.minPrice()));
            }
            if (filter.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.maxPrice()));
            }

            // ── Zona (location) — solo en data query, no en count ─────────
            if (filter.locationId() != null) {
                if (isCountQuery) {
                    // En count query usamos subquery para evitar problemas con paginación
                    predicates.add(cb.equal(root.get("location").get("id"), filter.locationId()));
                } else {
                    Join<Object, Object> locationJoin = root.join("location", JoinType.LEFT);
                    predicates.add(cb.equal(locationJoin.get("id"), filter.locationId()));
                }
            }

            // ── Baños (mínimo) ────────────────────────────────────────────
            if (filter.minBathrooms() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bathrooms"), filter.minBathrooms()));
            }

            // ── Dormitorios (mínimo) ──────────────────────────────────────
            if (filter.minBedrooms() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bedrooms"), filter.minBedrooms()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
