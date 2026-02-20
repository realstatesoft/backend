package com.openroof.openroof.model.enums;

import lombok.Getter;

@Getter
public enum Priority {
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    private final int weight;

    Priority(int weight) {
        this.weight = weight;
    }

    public boolean isHigherThan(Priority other) {
        return this.weight > other.weight;
    }
}
