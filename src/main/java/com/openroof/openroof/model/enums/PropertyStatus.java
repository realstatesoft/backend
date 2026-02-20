package com.openroof.openroof.model.enums;

import java.util.Map;
import java.util.Set;

public enum PropertyStatus {
    PENDING,
    APPROVED,
    REJECTED,
    PUBLISHED,
    SOLD,
    RENTED,
    ARCHIVED;

    private static final Map<PropertyStatus, Set<PropertyStatus>> TRANSITIONS = Map.of(
            PENDING, Set.of(APPROVED, REJECTED),
            APPROVED, Set.of(PUBLISHED, ARCHIVED),
            REJECTED, Set.of(PENDING),
            PUBLISHED, Set.of(SOLD, RENTED, ARCHIVED),
            SOLD, Set.of(ARCHIVED),
            RENTED, Set.of(ARCHIVED, PUBLISHED),
            ARCHIVED, Set.of()
    );

    public boolean canTransitionTo(PropertyStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public PropertyStatus transitionTo(PropertyStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                    "No se puede transicionar de " + this + " a " + target);
        }
        return target;
    }

    public boolean isTerminal() {
        return this == SOLD || this == ARCHIVED;
    }

    public boolean isActive() {
        return this == PUBLISHED || this == RENTED;
    }
}
