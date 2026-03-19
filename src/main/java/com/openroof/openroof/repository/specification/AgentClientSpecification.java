package com.openroof.openroof.repository.specification;

import com.openroof.openroof.dto.agent.AgentClientSearchRequest;
import com.openroof.openroof.model.agent.AgentClient;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class AgentClientSpecification {

    public static Specification<AgentClient> filterBy(Long agentId, AgentClientSearchRequest criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Mandatory: only clients of the given agent
            predicates.add(cb.equal(root.get("agent").get("id"), agentId));

            if (criteria != null) {
                // Search by query (name or email)
                if (StringUtils.hasText(criteria.q())) {
                    Join<AgentClient, User> userJoin = root.join("user");
                    String pattern = "%" + criteria.q().toLowerCase() + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(userJoin.get("name")), pattern),
                            cb.like(cb.lower(userJoin.get("email")), pattern)
                    ));
                }

                // Filter by status
                if (criteria.status() != null) {
                    predicates.add(cb.equal(root.get("status"), criteria.status()));
                }

                // Filter by client type
                if (criteria.clientType() != null) {
                    predicates.add(cb.equal(root.get("clientType"), criteria.clientType()));
                }

                // Filter by creation date range
                if (criteria.createdAtFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.createdAtFrom()));
                }
                if (criteria.createdAtTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.createdAtTo()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
