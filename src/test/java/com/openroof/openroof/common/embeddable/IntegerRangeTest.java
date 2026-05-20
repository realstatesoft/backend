package com.openroof.openroof.common.embeddable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@code IntegerRange — contains(int)}.
 */
@DisplayName("IntegerRange — contains(int)")
class IntegerRangeTest {

    // ─── both bounds set ─────────────────────────────────────────────────────

    /**
     * value within [min, max] returns true.
     */
    @Test
    @DisplayName("value within [min, max] returns true")
    void contains_valueInRange_returnsTrue() {
        IntegerRange range = new IntegerRange(2, 5);
        assertThat(range.contains(3)).isTrue();
    }

    /**
     * value equal to min returns true (inclusive lower bound).
     */
    @Test
    @DisplayName("value equal to min returns true (inclusive lower bound)")
    void contains_valueEqualToMin_returnsTrue() {
        IntegerRange range = new IntegerRange(2, 5);
        assertThat(range.contains(2)).isTrue();
    }

    /**
     * value equal to max returns true (inclusive upper bound).
     */
    @Test
    @DisplayName("value equal to max returns true (inclusive upper bound)")
    void contains_valueEqualToMax_returnsTrue() {
        IntegerRange range = new IntegerRange(2, 5);
        assertThat(range.contains(5)).isTrue();
    }

    /**
     * value below min returns false.
     */
    @Test
    @DisplayName("value below min returns false")
    void contains_valueBelowMin_returnsFalse() {
        IntegerRange range = new IntegerRange(2, 5);
        assertThat(range.contains(1)).isFalse();
    }

    /**
     * value above max returns false.
     */
    @Test
    @DisplayName("value above max returns false")
    void contains_valueAboveMax_returnsFalse() {
        IntegerRange range = new IntegerRange(2, 5);
        assertThat(range.contains(6)).isFalse();
    }

    // ─── null min (unbounded lower) ──────────────────────────────────────────

    /**
     * null min — value smaller than max returns true.
     */
    @Test
    @DisplayName("null min — value smaller than max returns true")
    void contains_nullMin_valueUnderMax_returnsTrue() {
        IntegerRange range = new IntegerRange(null, 5);
        assertThat(range.contains(Integer.MIN_VALUE)).isTrue();
    }

    /**
     * null min — value above max returns false.
     */
    @Test
    @DisplayName("null min — value above max returns false")
    void contains_nullMin_valueAboveMax_returnsFalse() {
        IntegerRange range = new IntegerRange(null, 5);
        assertThat(range.contains(6)).isFalse();
    }

    // ─── null max (unbounded upper) ──────────────────────────────────────────

    /**
     * null max — value above min returns true.
     */
    @Test
    @DisplayName("null max — value above min returns true")
    void contains_nullMax_valueAboveMin_returnsTrue() {
        IntegerRange range = new IntegerRange(2, null);
        assertThat(range.contains(Integer.MAX_VALUE)).isTrue();
    }

    /**
     * null max — value below min returns false.
     */
    @Test
    @DisplayName("null max — value below min returns false")
    void contains_nullMax_valueBelowMin_returnsFalse() {
        IntegerRange range = new IntegerRange(2, null);
        assertThat(range.contains(1)).isFalse();
    }

    // ─── both null (fully unbounded) ─────────────────────────────────────────

    /**
     * both bounds null — any integer returns true.
     */
    @Test
    @DisplayName("both bounds null — any integer returns true")
    void contains_bothBoundsNull_anyValueReturnsTrue() {
        IntegerRange range = new IntegerRange(null, null);
        assertThat(range.contains(0)).isTrue();
        assertThat(range.contains(Integer.MIN_VALUE)).isTrue();
        assertThat(range.contains(Integer.MAX_VALUE)).isTrue();
    }

    // ─── edge: min == max ────────────────────────────────────────────────────

    /**
     * min equals max — only that exact value returns true.
     */
    @Test
    @DisplayName("min equals max — only that exact value returns true")
    void contains_minEqualsMax_onlyExactValueReturnsTrue() {
        IntegerRange range = new IntegerRange(3, 3);
        assertThat(range.contains(3)).isTrue();
        assertThat(range.contains(2)).isFalse();
        assertThat(range.contains(4)).isFalse();
    }
}
