package com.openroof.openroof.model.enums;

import java.util.Map;
import java.util.Set;

public enum VisitStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
    NO_SHOW;

    private static final Map<VisitStatus, Set<VisitStatus>> TRANSITIONS = Map.of(
            PENDING, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(COMPLETED, CANCELLED, NO_SHOW),
            CANCELLED, Set.of(),
            COMPLETED, Set.of(),
            NO_SHOW, Set.of()
    );

    public boolean canTransitionTo(VisitStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public VisitStatus transitionTo(VisitStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                    "No se puede transicionar de " + this + " a " + target);
        }
        return target;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == NO_SHOW;
    }
}
