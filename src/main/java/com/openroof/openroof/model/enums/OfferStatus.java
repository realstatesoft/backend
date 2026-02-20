package com.openroof.openroof.model.enums;

import java.util.Map;
import java.util.Set;

public enum OfferStatus {
    SENT,
    VIEWED,
    NEGOTIATING,
    ACCEPTED,
    REJECTED,
    EXPIRED;

    private static final Map<OfferStatus, Set<OfferStatus>> TRANSITIONS = Map.of(
            SENT, Set.of(VIEWED, REJECTED, EXPIRED),
            VIEWED, Set.of(NEGOTIATING, ACCEPTED, REJECTED, EXPIRED),
            NEGOTIATING, Set.of(ACCEPTED, REJECTED, EXPIRED),
            ACCEPTED, Set.of(),
            REJECTED, Set.of(),
            EXPIRED, Set.of()
    );

    public boolean canTransitionTo(OfferStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public OfferStatus transitionTo(OfferStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                    "No se puede transicionar de " + this + " a " + target);
        }
        return target;
    }

    public boolean isTerminal() {
        return this == ACCEPTED || this == REJECTED || this == EXPIRED;
    }
}
