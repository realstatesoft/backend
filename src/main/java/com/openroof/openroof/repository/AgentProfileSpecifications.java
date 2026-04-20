package com.openroof.openroof.repository;

import com.openroof.openroof.model.agent.AgentProfile;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Type-safe JPA Specifications for {@link AgentProfile} queries.
 *
 * <p>Usage example in the service:
 * <pre>{@code
 * Specification<AgentProfile> spec = Specification.where(null);
 * if (keyword != null) spec = spec.and(AgentProfileSpecifications.hasKeyword(keyword));
 * if (specialty != null) spec = spec.and(AgentProfileSpecifications.hasSpecialty(specialty));
 * if (minRating != null) spec = spec.and(AgentProfileSpecifications.hasMinRating(minRating));
 * Page<AgentProfile> page = agentProfileRepository.findAll(spec, pageable);
 * }</pre>
 */
public final class AgentProfileSpecifications {

    private AgentProfileSpecifications() { /* utility class */ }

    /**
     * Matches agents whose name, companyName or licenseNumber contains the keyword
     * (case-insensitive). Input is escaped so {@code '%'}, {@code '_'} and {@code '\'}
     * are treated as literals.
     */
    public static Specification<AgentProfile> hasKeyword(String rawKeyword) {
        return (root, query, cb) -> {
            // Always fetch the user join so we don't trigger N+1 on the result page
            root.fetch("user", JoinType.INNER);

            String escaped = rawKeyword.trim()
                    .replace("\\", "\\\\")
                    .replace("%",  "\\%")
                    .replace("_",  "\\_");
            String pattern = "%" + escaped.toLowerCase() + "%";

            Join<?, ?> user = root.join("user", JoinType.INNER);
            return cb.or(
                    cb.like(cb.lower(user.get("name")),          pattern, '\\'),
                    cb.like(cb.lower(root.get("companyName")),   pattern, '\\'),
                    cb.like(cb.lower(root.get("licenseNumber")), pattern, '\\')
            );
        };
    }

    /**
     * Matches agents that have at least one specialty whose name equals the given
     * value (case-insensitive, exact match).
     */
    public static Specification<AgentProfile> hasSpecialty(String specialty) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<AgentProfile> subRoot = sub.from(AgentProfile.class);
            Join<?, ?> specs = subRoot.join("specialties");
            sub.select(subRoot.get("id"))
               .where(cb.and(
                   cb.equal(subRoot.get("id"), root.get("id")),
                   cb.equal(cb.lower(specs.get("name")), specialty.trim().toLowerCase())
               ));
            return cb.exists(sub);
        };
    }

    /**
     * Matches agents whose {@code avgRating >= minRating}.
     */
    public static Specification<AgentProfile> hasMinRating(BigDecimal minRating) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("avgRating"), minRating);
    }
}
