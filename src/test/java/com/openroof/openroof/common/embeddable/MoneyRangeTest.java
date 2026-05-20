package com.openroof.openroof.common.embeddable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MoneyRange — contains()")
class MoneyRangeTest {

    // ─── null value ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("null value always returns false")
    void contains_nullValue_returnsFalse() {
        MoneyRange range = new MoneyRange(new BigDecimal("100"), new BigDecimal("500"));
        assertThat(range.contains(null)).isFalse();
    }

    // ─── both bounds set ─────────────────────────────────────────────────────

    @Test
    @DisplayName("value within [min, max] returns true")
    void contains_valueInRange_returnsTrue() {
        MoneyRange range = new MoneyRange(new BigDecimal("100"), new BigDecimal("500"));
        assertThat(range.contains(new BigDecimal("300"))).isTrue();
    }

    @Test
    @DisplayName("value equal to min returns true (inclusive lower bound)")
    void contains_valueEqualToMin_returnsTrue() {
        MoneyRange range = new MoneyRange(new BigDecimal("100"), new BigDecimal("500"));
        assertThat(range.contains(new BigDecimal("100"))).isTrue();
    }

    @Test
    @DisplayName("value equal to max returns true (inclusive upper bound)")
    void contains_valueEqualToMax_returnsTrue() {
        MoneyRange range = new MoneyRange(new BigDecimal("100"), new BigDecimal("500"));
        assertThat(range.contains(new BigDecimal("500"))).isTrue();
    }

    @Test
    @DisplayName("value below min returns false")
    void contains_valueBelowMin_returnsFalse() {
        MoneyRange range = new MoneyRange(new BigDecimal("100"), new BigDecimal("500"));
        assertThat(range.contains(new BigDecimal("99.99"))).isFalse();
    }

    @Test
    @DisplayName("value above max returns false")
    void contains_valueAboveMax_returnsFalse() {
        MoneyRange range = new MoneyRange(new BigDecimal("100"), new BigDecimal("500"));
        assertThat(range.contains(new BigDecimal("500.01"))).isFalse();
    }

    // ─── null min (unbounded lower) ──────────────────────────────────────────

    @Test
    @DisplayName("null min means no lower bound — very small value returns true")
    void contains_nullMin_noLowerBound_returnsTrue() {
        MoneyRange range = new MoneyRange(null, new BigDecimal("500"));
        assertThat(range.contains(new BigDecimal("0.01"))).isTrue();
    }

    @Test
    @DisplayName("null min — value above max still returns false")
    void contains_nullMin_valueAboveMax_returnsFalse() {
        MoneyRange range = new MoneyRange(null, new BigDecimal("500"));
        assertThat(range.contains(new BigDecimal("501"))).isFalse();
    }

    // ─── null max (unbounded upper) ──────────────────────────────────────────

    @Test
    @DisplayName("null max means no upper bound — very large value returns true")
    void contains_nullMax_noUpperBound_returnsTrue() {
        MoneyRange range = new MoneyRange(new BigDecimal("100"), null);
        assertThat(range.contains(new BigDecimal("999999999"))).isTrue();
    }

    @Test
    @DisplayName("null max — value below min still returns false")
    void contains_nullMax_valueBelowMin_returnsFalse() {
        MoneyRange range = new MoneyRange(new BigDecimal("100"), null);
        assertThat(range.contains(new BigDecimal("99"))).isFalse();
    }

    // ─── both null (fully unbounded) ─────────────────────────────────────────

    @Test
    @DisplayName("both bounds null — any non-null value returns true")
    void contains_bothBoundsNull_anyValueReturnsTrue() {
        MoneyRange range = new MoneyRange(null, null);
        assertThat(range.contains(new BigDecimal("0"))).isTrue();
        assertThat(range.contains(new BigDecimal("1000000"))).isTrue();
    }
}
