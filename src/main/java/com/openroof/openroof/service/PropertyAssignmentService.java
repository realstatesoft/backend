package com.openroof.openroof.service;

import com.openroof.openroof.dto.property.AssignPropertyRequest;
import com.openroof.openroof.dto.property.PropertyAssignmentResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.AssignmentStatus;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.property.PropertyAssignment;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.PropertyAssignmentRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PropertyAssignmentService {

    private final PropertyAssignmentRepository assignmentRepository;
    private final PropertyRepository propertyRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // ─── ASSIGN (owner) ───────────────────────────────────────────

    public PropertyAssignmentResponse assign(Long propertyId, AssignPropertyRequest request, String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        Property property = getProperty(propertyId);

        validateIsOwner(property, currentUser);

        AgentProfile agent = agentProfileRepository.findById(request.agentProfileId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de agente no encontrado con ID: " + request.agentProfileId()));

        // Block duplicate active assignments
        assignmentRepository.findActiveByPropertyAndAgent(
                propertyId, request.agentProfileId(),
                List.of(AssignmentStatus.PENDING, AssignmentStatus.ACCEPTED))
                .ifPresent(existing -> {
                    throw new BadRequestException(
                            "Ya existe una asignación activa para este agente en esta propiedad");
                });

        PropertyAssignment assignment = PropertyAssignment.builder()
                .property(property)
                .agent(agent)
                .assignedBy(currentUser)
                .status(AssignmentStatus.PENDING)
                .assignedAt(LocalDateTime.now())
                .build();

        PropertyAssignmentResponse response = toResponse(assignmentRepository.save(assignment));
        emailService.sendPropertyAssignmentEmail(
                agent.getUser().getEmail(), agent.getUser().getName(),
                property.getTitle(), currentUser.getName());
        return response;
    }

    // ─── ACCEPT / REJECT (agent) ──────────────────────────────────

    public PropertyAssignmentResponse accept(Long assignmentId, String currentUserEmail) {
        return respond(assignmentId, currentUserEmail, AssignmentStatus.ACCEPTED);
    }

    public PropertyAssignmentResponse reject(Long assignmentId, String currentUserEmail) {
        return respond(assignmentId, currentUserEmail, AssignmentStatus.REJECTED);
    }

    private PropertyAssignmentResponse respond(Long assignmentId, String currentUserEmail, AssignmentStatus newStatus) {
        User currentUser = getUserByEmail(currentUserEmail);
        PropertyAssignment assignment = getAssignment(assignmentId);

        validateIsAssignedAgent(assignment, currentUser);

        if (assignment.getStatus() != AssignmentStatus.PENDING) {
            throw new BadRequestException(
                    "Solo se puede responder a asignaciones en estado PENDING. Estado actual: " + assignment.getStatus());
        }

        if (newStatus == AssignmentStatus.ACCEPTED) {
            Property lockedProperty = propertyRepository.findByIdForUpdate(assignment.getProperty().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Propiedad no encontrada con ID: " + assignment.getProperty().getId()));

            boolean hasAnotherAccepted = assignmentRepository.existsByPropertyAndStatusExcludingAssignment(
                    lockedProperty.getId(), AssignmentStatus.ACCEPTED, assignment.getId());

            if (hasAnotherAccepted) {
                throw new BadRequestException(
                        "Ya existe una asignación ACCEPTED para esta propiedad. Revoca o rechaza la asignación activa antes de aceptar otra.");
            }

            lockedProperty.setAgent(assignment.getAgent());
            propertyRepository.save(lockedProperty);
        }

        assignment.setStatus(newStatus);

        try {
            PropertyAssignmentResponse response = toResponse(assignmentRepository.save(assignment));
            emailService.sendPropertyAssignmentResponseEmail(
                    assignment.getProperty().getOwner().getEmail(),
                    assignment.getProperty().getOwner().getName(),
                    assignment.getProperty().getTitle(),
                    assignment.getAgent().getUser().getName(),
                    newStatus.name());
            return response;
        } catch (DataIntegrityViolationException ex) {
            if (newStatus == AssignmentStatus.ACCEPTED) {
                throw new BadRequestException(
                        "Ya existe una asignación ACCEPTED para esta propiedad. Revoca o rechaza la asignación activa antes de aceptar otra.");
            }
            throw ex;
        }
    }

    // ─── REVOKE (owner) ───────────────────────────────────────────

    public PropertyAssignmentResponse revoke(Long assignmentId, String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        PropertyAssignment assignment = getAssignment(assignmentId);

        validateIsOwner(assignment.getProperty(), currentUser);

        if (assignment.getStatus() == AssignmentStatus.REVOKED
                || assignment.getStatus() == AssignmentStatus.REJECTED) {
            throw new BadRequestException(
                    "No se puede revocar una asignación en estado: " + assignment.getStatus());
        }

        if (assignment.getStatus() == AssignmentStatus.ACCEPTED) {
            Property property = assignment.getProperty();
            if (property.getAgent() != null
                    && property.getAgent().getId().equals(assignment.getAgent().getId())) {
                property.setAgent(null);
                propertyRepository.save(property);
            }
        }

        assignment.setStatus(AssignmentStatus.REVOKED);
        return toResponse(assignmentRepository.save(assignment));
    }

    // ─── QUERIES ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PropertyAssignmentResponse> getByProperty(Long propertyId, String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        Property property = getProperty(propertyId);

        if (!isOwnerOrAdmin(property, currentUser)) {
            throw new BadRequestException("No tienes permiso para ver las asignaciones de esta propiedad");
        }

        return assignmentRepository.findByProperty_Id(propertyId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PropertyAssignmentResponse> getMyAssignments(String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);

        AgentProfile agentProfile = agentProfileRepository.findByUser_Id(currentUser.getId())
                .orElseThrow(() -> new BadRequestException(
                        "No tienes un perfil de agente asociado a tu cuenta"));

        return assignmentRepository.findByAgent_IdAndStatusIn(
                agentProfile.getId(),
                List.of(AssignmentStatus.PENDING, AssignmentStatus.ACCEPTED)
            ).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── HELPERS ──────────────────────────────────────────────────

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
    }

    private Property getProperty(Long propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Propiedad no encontrada con ID: " + propertyId));
    }

    private PropertyAssignment getAssignment(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asignación no encontrada con ID: " + assignmentId));
    }

    private void validateIsOwner(Property property, User currentUser) {
        if (!isOwnerOrAdmin(property, currentUser)) {
            throw new BadRequestException("Solo el propietario puede gestionar las asignaciones de esta propiedad");
        }
    }

    private void validateIsAssignedAgent(PropertyAssignment assignment, User currentUser) {
        if (!assignment.getAgent().getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Solo el agente asignado puede responder a esta solicitud");
        }
    }

    private boolean isOwnerOrAdmin(Property property, User currentUser) {
        return property.getOwner().getId().equals(currentUser.getId())
                || currentUser.getRole() == UserRole.ADMIN;
    }

    private PropertyAssignmentResponse toResponse(PropertyAssignment a) {
        return new PropertyAssignmentResponse(
                a.getId(),
                a.getProperty().getId(),
                a.getProperty().getTitle(),
                a.getAgent().getId(),
                a.getAgent().getUser().getId(),
                a.getAgent().getUser().getName(),
                a.getAssignedBy().getId(),
                a.getAssignedBy().getName(),
                a.getStatus(),
                a.getAssignedAt(),
                a.getCreatedAt()
        );
    }
}
