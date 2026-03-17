package com.openroof.openroof.dto.visit;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CounterProposeRequest(

        @NotNull(message = "La nueva fecha propuesta es obligatoria")
        @Future(message = "La nueva fecha propuesta debe ser futura")
        LocalDateTime counterProposedAt,

        String counterProposeMessage
) {}
