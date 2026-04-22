package com.openroof.openroof.dto.dashboard;

public record CountStatItem(long value, double trend) {
    public static CountStatItem of(long value, double trend) {
        return new CountStatItem(value, trend);
    }
}
