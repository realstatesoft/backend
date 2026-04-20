package com.openroof.openroof.service;

import com.openroof.openroof.dto.agent.*;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.AgentProfileMapper;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.agent.AgentSpecialty;
import com.openroof.openroof.model.enums.PropertyCategory;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.AgentSpecialtyRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AgentProfileService {

    private final AgentProfileRepository agentProfileRepository;
    private final UserRepository userRepository;
    private final AgentSpecialtyRepository agentSpecialtyRepository;
    private final AgentProfileMapper agentProfileMapper;

    // ─── CREATE ───────────────────────────────────────────────────

    public AgentProfileResponse create(CreateAgentProfileRequest request) {
        // Validar que el usuario existe
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con ID: " + request.userId()));

        // Validar que el usuario no tenga ya un perfil de agente
        if (agentProfileRepository.existsByUser_Id(request.userId())) {
            throw new BadRequestException(
                    "El usuario con ID " + request.userId() + " ya tiene un perfil de agente");
        }

        // Resolver especialidades (opcional)
        List<AgentSpecialty> specialties = resolveSpecialties(request.specialtyIds());

        // Crear entidad
        AgentProfile agent = agentProfileMapper.toEntity(request, user, specialties);

        // Asegurar que el rol del usuario sea AGENT
        if (user.getRole() != UserRole.AGENT) {
            user.setRole(UserRole.AGENT);
            userRepository.save(user);
        }

        agent = agentProfileRepository.save(agent);
        return agentProfileMapper.toResponse(agent);
    }

    // ─── READ ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AgentProfileResponse getById(Long id) {
        AgentProfile agent = findAgentOrThrow(id);
        return agentProfileMapper.toResponse(agent);
    }

    @Transactional(readOnly = true)
    public Page<AgentProfileSummaryResponse> getAll(Pageable pageable) {
        return agentProfileRepository.findAllWithUser(pageable)
                .map(agentProfileMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<AgentProfileSummaryResponse> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return agentProfileRepository.findAllWithUser(pageable)
                    .map(agentProfileMapper::toSummaryResponse);
        }
        return agentProfileRepository.searchByKeyword(keyword.trim(), pageable)
                .map(agentProfileMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<AgentProfileSummaryResponse> searchWithFilters(
            String keyword, String specialty, java.math.BigDecimal minRating, Pageable pageable) {

        String kw = (keyword  != null && !keyword.isBlank())
                ? "%" + keyword.trim() + "%"
                : "%";                                      // '%' matches everything
        String sp = (specialty != null && !specialty.isBlank())
                ? specialty.trim()
                : "";                                       // '' means no specialty filter
        java.math.BigDecimal rating = (minRating != null)
                ? minRating
                : java.math.BigDecimal.valueOf(-1);         // -1 means no rating filter

        return agentProfileRepository.searchWithFilters(kw, sp, rating, pageable)
                .map(agentProfileMapper::toSummaryResponse);
    }

    // ─── UPDATE ───────────────────────────────────────────────────

    public AgentProfileResponse update(Long id, UpdateAgentProfileRequest request) {
        AgentProfile agent = findAgentOrThrow(id);

        // Actualizar campos básicos
        agentProfileMapper.updateEntity(agent, request);

        // Reemplazar especialidades (si se envían)
        if (request.specialtyIds() != null) {
            List<AgentSpecialty> specialties = resolveSpecialties(request.specialtyIds());
            agent.setSpecialties(specialties);
        }

        // Reemplazar redes sociales (si se envían)
        if (request.socialMedia() != null) {
            agentProfileMapper.replaceSocialMedia(agent, request.socialMedia());
        }

        agent = agentProfileRepository.save(agent);
        return agentProfileMapper.toResponse(agent);
    }

    // ─── DELETE (Soft) ────────────────────────────────────────────

    public void delete(Long id) {
        AgentProfile agent = findAgentOrThrow(id);
        agent.softDelete();
        agentProfileRepository.save(agent);
    }

    // ─── SUGGESTED AGENTS ─────────────────────────────────────────

    /**
     * Obtiene agentes sugeridos basados en el tipo de propiedad y categoría.
     * La lógica busca agentes cuyas especialidades coincidan con el tipo de propiedad.
     */
    @Transactional(readOnly = true)
    public List<AgentProfileSummaryResponse> getSuggestedAgents(SuggestedAgentsRequest request) {
        int limit = request.limit() != null ? request.limit() : 5;

        // 1. Construir lista de especialidades relevantes basadas en el tipo de propiedad
        List<String> relevantSpecialties = buildRelevantSpecialties(
                request.propertyType(),
                request.category()
        );

        List<AgentProfile> agents;

        if (!relevantSpecialties.isEmpty()) {
            // 2. Buscar agentes con especialidades coincidentes (limitando en DB)
            agents = agentProfileRepository.findBySpecialtyNamesOrderByRating(
                    relevantSpecialties,
                    PageRequest.of(0, limit)
            );
        } else {
            // 3. Si no hay especialidades específicas, obtener los mejor calificados
            agents = agentProfileRepository.findTopAgentsOrderByRating(PageRequest.of(0, limit));
        }

        // 4. Mapear a DTO (el límite ya fue aplicado en la consulta a DB)
        return agents.stream()
                .map(agentProfileMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Construye una lista de nombres de especialidades relevantes basados en el tipo de propiedad.
     */
    private List<String> buildRelevantSpecialties(PropertyType propertyType, PropertyCategory category) {
        List<String> specialties = new ArrayList<>();

        // Mapeo de PropertyType a especialidades comunes
        if (propertyType != null) {
            switch (propertyType) {
                case HOUSE -> {
                    specialties.add("residencial");
                    specialties.add("casas");
                    specialties.add("residential");
                }
                case APARTMENT -> {
                    specialties.add("residencial");
                    specialties.add("departamentos");
                    specialties.add("apartamentos");
                    specialties.add("residential");
                }
                case LAND -> {
                    specialties.add("terrenos");
                    specialties.add("lotes");
                    specialties.add("land");
                }
                case OFFICE -> {
                    specialties.add("comercial");
                    specialties.add("oficinas");
                    specialties.add("commercial");
                }
                case WAREHOUSE -> {
                    specialties.add("industrial");
                    specialties.add("depósitos");
                    specialties.add("bodegas");
                    specialties.add("commercial");
                }
                case FARM -> {
                    specialties.add("rural");
                    specialties.add("campos");
                    specialties.add("estancias");
                    specialties.add("agrícola");
                }
            }
        }

        // Agregar especialidades por categoría
        if (category != null) {
            switch (category) {
                case SALE -> {
                    specialties.add("ventas");
                    specialties.add("sales");
                }
                case RENT -> {
                    specialties.add("alquileres");
                    specialties.add("rentals");
                }
                case SALE_OR_RENT -> {
                    specialties.add("ventas");
                    specialties.add("alquileres");
                }
            }
        }

        return specialties;
    }

    // ─── Helpers privados ─────────────────────────────────────────

    private AgentProfile findAgentOrThrow(Long id) {
        return agentProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Agente no encontrado con ID: " + id));
    }

    private List<AgentSpecialty> resolveSpecialties(List<Long> specialtyIds) {
        if (specialtyIds == null || specialtyIds.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(specialtyIds);
        List<AgentSpecialty> specialties = agentSpecialtyRepository.findAllById(uniqueIds);
        if (specialties.size() != uniqueIds.size()) {
            throw new BadRequestException("Algunas especialidades no fueron encontradas");
        }
        return specialties;
    }
}
