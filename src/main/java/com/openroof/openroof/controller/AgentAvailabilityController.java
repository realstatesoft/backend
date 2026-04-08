package com.openroof.openroof.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.agent.BusySlotResponse;
import com.openroof.openroof.service.AgentAvailabilityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
@Tag(name = "Agent Availability", description = "Consulta de disponibilidad de horarios de agentes")
public class AgentAvailabilityController {

    private final AgentAvailabilityService agentAvailabilityService;

    @GetMapping("/{agentId}/availability")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener los slots ocupados de un agente en una fecha determinada")
    public ResponseEntity<ApiResponse<List<BusySlotResponse>>> getAvailability(
            @Parameter(description = "ID del perfil de agente") @PathVariable Long agentId,
            @Parameter(description = "Fecha en formato YYYY-MM-DD")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<BusySlotResponse> busySlots = agentAvailabilityService.getBusySlots(agentId, date);
        return ResponseEntity.ok(ApiResponse.ok(busySlots));
    }
}
