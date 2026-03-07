package com.openroof.openroof.service;

import com.openroof.openroof.dto.agent.AgentSpecialtyResponse;
import com.openroof.openroof.dto.agent.CreateAgentSpecialtyRequest;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.model.agent.AgentSpecialty;
import com.openroof.openroof.repository.AgentSpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentSpecialtyService {

    private final AgentSpecialtyRepository agentSpecialtyRepository;

    @Transactional
    public AgentSpecialtyResponse create(CreateAgentSpecialtyRequest request) {
        if (agentSpecialtyRepository.existsByName(request.name().trim())) {
            throw new BadRequestException("La especialidad ya existe: " + request.name());
        }

        AgentSpecialty spec = AgentSpecialty.builder()
                .name(request.name().trim())
                .build();

        spec = agentSpecialtyRepository.save(spec);
        return new AgentSpecialtyResponse(spec.getId(), spec.getName());
    }

    @Transactional(readOnly = true)
    public List<AgentSpecialtyResponse> getAll() {
        return agentSpecialtyRepository.findAll().stream()
                .map(s -> new AgentSpecialtyResponse(s.getId(), s.getName()))
                .collect(Collectors.toList());
    }
}
