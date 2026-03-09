package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.agent.AgentSpecialtyResponse;
import com.openroof.openroof.dto.agent.CreateAgentSpecialtyRequest;
import com.openroof.openroof.service.AgentSpecialtyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agents/specialties")
@RequiredArgsConstructor
@Tag(name = "Agent Specialties", description = "Gestión de especialidades de agentes")
public class AgentSpecialtyController {

    private final AgentSpecialtyService agentSpecialtyService;

    @GetMapping
    @Operation(summary = "Listar todas las especialidades")
    public ResponseEntity<ApiResponse<List<AgentSpecialtyResponse>>> getAll() {
        List<AgentSpecialtyResponse> list = agentSpecialtyService.getAll();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear una especialidad (solo ADMIN)")
    public ResponseEntity<ApiResponse<AgentSpecialtyResponse>> create(
            @Valid @RequestBody CreateAgentSpecialtyRequest request) {

        AgentSpecialtyResponse resp = agentSpecialtyService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(resp, "Especialidad creada exitosamente"));
    }
}
