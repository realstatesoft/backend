package com.openroof.openroof.audit;

import com.openroof.openroof.model.admin.AuditLog;
import com.openroof.openroof.model.enums.AuditAction;
import com.openroof.openroof.model.enums.AuditEntityType;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AuditLogSpecification {

    private AuditLogSpecification() {
    }

    public static Specification<AuditLog> build(
            Long userId,
            String userSearch,
            AuditEntityType entityType,
            Long entityId,
            AuditAction action,
            LocalDateTime createdFrom,
            LocalDateTime createdTo) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            boolean filterByUserId = userId != null;
            boolean filterByUserSearch = userSearch != null && !userSearch.isBlank();

            if (filterByUserId || filterByUserSearch) {
                var userJoin = root.join("user", JoinType.LEFT);
                if (filterByUserId) {
                    predicates.add(cb.equal(userJoin.get("id"), userId));
                }
                if (filterByUserSearch) {
                    String term = userSearch.trim().toLowerCase(Locale.ROOT);
                    String pattern = "%" + term + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(cb.coalesce(userJoin.get("name"), cb.literal(""))), pattern),
                            cb.like(cb.lower(userJoin.get("email")), pattern)));
                }
            }
            if (entityType != null) {
                predicates.add(cb.equal(root.get("entityType"), entityType.name()));
            }
            if (entityId != null) {
                predicates.add(cb.equal(root.get("entityId"), entityId));
            }
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action.name()));
            }
            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
