package com.openroof.openroof.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Body completo del webhook single_buy_confirm: {"operation": {...}}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BancardConfirmRequest(
        BancardConfirmOperation operation
) {}
