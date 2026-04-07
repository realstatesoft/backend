package com.openroof.openroof.service;

import com.openroof.openroof.dto.agent.*;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.agent.*;
import com.openroof.openroof.model.enums.*;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.ExternalClientRepository;
import com.openroof.openroof.common.embeddable.IntegerRange;
import com.openroof.openroof.common.embeddable.MoneyRange;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExternalClientService {

    private final ExternalClientRepository externalClientRepository;
    private final AgentProfileRepository agentProfileRepository;

    public ExternalClientResponse create(CreateExternalClientRequest request, Long currentUserId) {
        Long agentId = request.agentId();
        if (agentId == null) {
            agentId = agentProfileRepository.findByUser_Id(currentUserId)
                    .map(AgentProfile::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró el perfil de agente para el usuario actual"));
        }

        final Long finalAgentId = agentId;
        AgentProfile agent = agentProfileRepository.findById(finalAgentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + finalAgentId));

        // Verify the authenticated user owns this agent profile
        if (!agent.getUser().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("No tiene permiso para gestionar clientes del agente con id: " + finalAgentId);
        }

        ExternalClient client = ExternalClient.builder()
                .agent(agent)
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .status(request.status() != null ? request.status() : ClientStatus.ACTIVE)
                .priority(request.priority() != null ? request.priority() : Priority.MEDIUM)
                .clientType(request.clientType() != null ? request.clientType() : ClientType.BUYER)
                .origin(request.sourceChannel()) // Using sourceChannel as origin
                .tags(new ArrayList<>())
                .notes(request.notes())
                .birthDate(request.birthDate())
                .maritalStatus(parseMaritalStatus(request.maritalStatus()))
                .occupation(request.occupation())
                .annualIncome(request.annualIncome())
                .address(request.address())
                .sourceChannel(request.sourceChannel())
                .budgetRange(new MoneyRange(request.minBudget(), request.maxBudget()))
                .bedroomRange(new IntegerRange(request.minBedrooms(), request.maxBedrooms()))
                .bathroomRange(new IntegerRange(request.minBathrooms(), request.maxBathrooms()))
                .preferredPropertyTypes(request.preferredPropertyTypes() != null ? request.preferredPropertyTypes() : new ArrayList<>())
                .preferredAreas(request.preferredAreas() != null ? request.preferredAreas() : new ArrayList<>())
                .desiredFeatures(request.desiredFeatures() != null ? request.desiredFeatures() : new ArrayList<>())
                .isSearchingProperty(request.isSearchingProperty() != null ? request.isSearchingProperty() : false)
                .build();

        client = externalClientRepository.save(client);
        return mapToResponse(client);
    }

    @Transactional(readOnly = true)
    public ExternalClientResponse getById(Long id) {
        return externalClientRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("External client not found"));
    }

    public ExternalClientResponse update(Long id, UpdateExternalClientRequest request) {
        ExternalClient client = externalClientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("External client not found"));

        if (request.name() != null) client.setName(request.name());
        if (request.email() != null) client.setEmail(request.email());
        if (request.phone() != null) client.setPhone(request.phone());
        if (request.status() != null) client.setStatus(request.status());
        if (request.priority() != null) client.setPriority(request.priority());
        if (request.clientType() != null) client.setClientType(request.clientType());
        if (request.lastContactDate() != null) client.setLastContactDate(request.lastContactDate());
        if (request.origin() != null) client.setOrigin(request.origin());
        if (request.tags() != null) client.setTags(request.tags());
        if (request.notes() != null) client.setNotes(request.notes());

        // New fields
        if (request.birthDate() != null) client.setBirthDate(request.birthDate());
        if (request.maritalStatus() != null) client.setMaritalStatus(parseMaritalStatus(request.maritalStatus()));
        if (request.occupation() != null) client.setOccupation(request.occupation());
        if (request.annualIncome() != null) client.setAnnualIncome(request.annualIncome());
        if (request.address() != null) client.setAddress(request.address());
        if (request.sourceChannel() != null) client.setSourceChannel(request.sourceChannel());
        if (request.isSearchingProperty() != null) client.setIsSearchingProperty(request.isSearchingProperty());

        if (request.minBudget() != null || request.maxBudget() != null) {
            BigDecimal min = request.minBudget() != null ? request.minBudget() : (client.getBudgetRange() != null ? client.getBudgetRange().getMin() : null);
            BigDecimal max = request.maxBudget() != null ? request.maxBudget() : (client.getBudgetRange() != null ? client.getBudgetRange().getMax() : null);
            client.setBudgetRange(new MoneyRange(min, max));
        }

        if (request.minBedrooms() != null || request.maxBedrooms() != null) {
            Integer min = request.minBedrooms() != null ? request.minBedrooms() : (client.getBedroomRange() != null ? client.getBedroomRange().getMin() : null);
            Integer max = request.maxBedrooms() != null ? request.maxBedrooms() : (client.getBedroomRange() != null ? client.getBedroomRange().getMax() : null);
            client.setBedroomRange(new IntegerRange(min, max));
        }

        if (request.minBathrooms() != null || request.maxBathrooms() != null) {
            Integer min = request.minBathrooms() != null ? request.minBathrooms() : (client.getBathroomRange() != null ? client.getBathroomRange().getMin() : null);
            Integer max = request.maxBathrooms() != null ? request.maxBathrooms() : (client.getBathroomRange() != null ? client.getBathroomRange().getMax() : null);
            client.setBathroomRange(new IntegerRange(min, max));
        }

        if (request.preferredPropertyTypes() != null) client.setPreferredPropertyTypes(request.preferredPropertyTypes());
        if (request.preferredAreas() != null) client.setPreferredAreas(request.preferredAreas());
        if (request.desiredFeatures() != null) client.setDesiredFeatures(request.desiredFeatures());

        client = externalClientRepository.save(client);
        return mapToResponse(client);
    }

    public void delete(Long id) {
        ExternalClient client = externalClientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("External client not found"));
        externalClientRepository.delete(client);
    }

    @Transactional(readOnly = true)
    public Page<UnifiedClientSummaryResponse> searchUnified(Long agentId, AgentClientSearchRequest criteria, Pageable pageable) {
        String statusStr = criteria.status() != null ? criteria.status().name() : null;
        String typeStr = criteria.clientType() != null ? criteria.clientType().name() : null;

        // Map frontend camelCase sort parameters to database snake_case columns
        java.util.List<String> validColumns = java.util.Arrays.asList(
                "name", "email", "phone", "status", "priority", "client_type", "last_contact_date", "created_at", "internal_type", "id"
        );

        java.util.List<Sort.Order> validOrders = pageable.getSort().stream()
                .map(order -> {
                    String prop = order.getProperty();
                    String mappedProp = prop;
                    if ("createdAt".equals(prop)) mappedProp = "created_at";
                    else if ("lastContactDate".equals(prop)) mappedProp = "last_contact_date";
                    else if ("clientType".equals(prop)) mappedProp = "client_type";
                    else if ("internalType".equals(prop)) mappedProp = "internal_type";
                    return new Sort.Order(order.getDirection(), mappedProp);
                })
                .filter(order -> validColumns.contains(order.getProperty()))
                .toList();

        Sort mappedSort = validOrders.isEmpty() ? Sort.by(Sort.Direction.DESC, "created_at") : Sort.by(validOrders);
        
        Pageable mappedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mappedSort);

        return externalClientRepository.searchUnifiedClients(agentId, criteria.q(), statusStr, typeStr, criteria.internalType(), mappedPageable)
                .map(this::mapToUnifiedResponse);
    }

    private ExternalClientResponse mapToResponse(ExternalClient client) {
        return new ExternalClientResponse(
                client.getId(),
                client.getAgent().getId(),
                client.getName(),
                client.getEmail(),
                client.getPhone(),
                client.getStatus().name(),
                client.getPriority().name(),
                client.getClientType().name(),
                client.getLastContactDate(),
                client.getOrigin(),
                client.getTags(),
                client.getNotes(),
                client.getBudgetRange() != null ? client.getBudgetRange().getMin() : null,
                client.getBudgetRange() != null ? client.getBudgetRange().getMax() : null,
                client.getBedroomRange() != null ? client.getBedroomRange().getMin() : null,
                client.getBedroomRange() != null ? client.getBedroomRange().getMax() : null,
                client.getBathroomRange() != null ? client.getBathroomRange().getMin() : null,
                client.getBathroomRange() != null ? client.getBathroomRange().getMax() : null,
                client.getMaritalStatus() != null ? client.getMaritalStatus().name() : null,
                client.getBirthDate(),
                client.getOccupation(),
                client.getAnnualIncome(),
                client.getAddress(),
                client.getSourceChannel(),
                client.getPreferredPropertyTypes(),
                client.getPreferredAreas(),
                client.getDesiredFeatures(),
                client.getIsSearchingProperty(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }

    private MaritalStatus parseMaritalStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MaritalStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            String validValues = Arrays.stream(MaritalStatus.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(
                    "Valor inválido para maritalStatus: '" + value + "'. Valores permitidos: [" + validValues + "]");
        }
    }

    private UnifiedClientSummaryResponse mapToUnifiedResponse(Map<String, Object> map) {
        return new UnifiedClientSummaryResponse(
                ((Number) map.get("id")).longValue(),
                map.get("user_id") != null ? ((Number) map.get("user_id")).longValue() : null,
                (String) map.get("name"),
                (String) map.get("email"),
                (String) map.get("phone"),
                (String) map.get("status"),
                (String) map.get("priority"),
                (String) map.get("client_type"),
                safeToLocalDateTime(map.get("last_contact_date")),
                safeToLocalDateTime(map.get("created_at")),
                (String) map.get("internal_type")
        );
    }

    private static LocalDateTime safeToLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof Timestamp ts) return ts.toLocalDateTime();
        if (value instanceof LocalDateTime ldt) return ldt;
        if (value instanceof java.time.OffsetDateTime odt) return odt.toLocalDateTime();
        if (value instanceof java.sql.Date d) return d.toLocalDate().atStartOfDay();
        if (value instanceof String s) return LocalDateTime.parse(s);
        throw new IllegalArgumentException("Cannot convert " + value.getClass().getName() + " to LocalDateTime");
    }
}
