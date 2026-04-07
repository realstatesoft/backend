package com.openroof.openroof.service;

import com.openroof.openroof.dto.visit.CounterProposeRequest;
import com.openroof.openroof.dto.visit.CreateVisitRequestRequest;
import com.openroof.openroof.dto.visit.VisitRequestResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.agent.AgentClient;
import com.openroof.openroof.model.enums.AssignmentStatus;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.enums.VisitRequestStatus;
import com.openroof.openroof.model.enums.VisitStatus;
import com.openroof.openroof.model.interaction.Visit;
import com.openroof.openroof.model.interaction.VisitRequest;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.AgentClientRepository;
import com.openroof.openroof.repository.PropertyAssignmentRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.repository.VisitRepository;
import com.openroof.openroof.repository.VisitRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VisitRequestService {

    private final VisitRequestRepository visitRequestRepository;
    private final VisitRepository visitRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final PropertyAssignmentRepository propertyAssignmentRepository;
    private final AgentClientRepository agentClientRepository;
    private final ClientInteractionService clientInteractionService;
    private final EmailService emailService;

    // ─── CREATE (buyer) ───────────────────────────────────────────

    public VisitRequestResponse create(CreateVisitRequestRequest request, String currentUserEmail) {
        User buyer = getUserByEmail(currentUserEmail);

        if (buyer.getRole() != UserRole.USER && buyer.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Solo un usuario puede crear una solicitud de visita");
        }

        Property property = getProperty(request.propertyId());

        AgentProfile agent = resolveAssignedAgent(property);

        VisitRequest visitRequest = VisitRequest.builder()
                .property(property)
                .buyer(buyer)
                .agent(agent)
                .proposedAt(request.proposedAt())
                .status(VisitRequestStatus.PENDING)
                .buyerName(request.buyerName() != null ? request.buyerName() : buyer.getName())
                .buyerEmail(request.buyerEmail() != null ? request.buyerEmail() : buyer.getEmail())
                .buyerPhone(request.buyerPhone() != null ? request.buyerPhone() : buyer.getPhone())
                .message(request.message())
                .build();

        // Notify agent (or property owner if no agent) — after commit so emails never fire on rollback
        if (agent != null) {
            String recipientEmail = agent.getUser().getEmail();
            String recipientName  = agent.getUser().getName();
            String propertyTitle  = property.getTitle();
            String buyerName      = visitRequest.getBuyerName();
            LocalDateTime proposedAt = visitRequest.getProposedAt();
            afterCommit(() -> emailService.sendVisitRequestCreatedEmail(
                    recipientEmail, recipientName, propertyTitle, buyerName, proposedAt));
        } else if (property.getOwner() != null) {
            String recipientEmail = property.getOwner().getEmail();
            String recipientName  = property.getOwner().getName();
            String propertyTitle  = property.getTitle();
            String buyerName      = visitRequest.getBuyerName();
            LocalDateTime proposedAt = visitRequest.getProposedAt();
            afterCommit(() -> emailService.sendVisitRequestCreatedEmail(
                    recipientEmail, recipientName, propertyTitle, buyerName, proposedAt));
        }

        return toResponse(visitRequestRepository.save(visitRequest));
    }

    // ─── ACCEPT (agent) → creates Visit ──────────────────────────

    public VisitRequestResponse accept(Long visitRequestId, String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        VisitRequest visitRequest = getVisitRequest(visitRequestId);

        validateIsAssignedAgent(visitRequest, currentUser);
        validatePendingOrCounterProposed(visitRequest);

        // Create the Visit
        Visit visit = Visit.builder()
                .property(visitRequest.getProperty())
                .buyer(visitRequest.getBuyer())
                .agent(visitRequest.getAgent())
                .scheduledAt(visitRequest.getCounterProposedAt() != null
                        ? visitRequest.getCounterProposedAt()
                        : visitRequest.getProposedAt())
                .status(VisitStatus.CONFIRMED)
                .build();

        visit = visitRepository.save(visit);

        visitRequest.setStatus(VisitRequestStatus.ACCEPTED);
        visitRequest.setVisit(visit);

        if (visitRequest.getAgent() != null) {
            boolean interactionRecorded = clientInteractionService.recordVisitConfirmed(
                    visitRequest.getAgent().getId(),
                    visitRequest.getBuyer().getId(),
                    visitRequest.getProperty().getId(),
                    visit.getScheduledAt());

            if (!interactionRecorded) {
                ensureAgentClientExists(visitRequest.getAgent(), visitRequest.getBuyer());
                clientInteractionService.recordVisitConfirmed(
                        visitRequest.getAgent().getId(),
                        visitRequest.getBuyer().getId(),
                        visitRequest.getProperty().getId(),
                        visit.getScheduledAt());
            }
        }

        String buyerEmail     = visitRequest.getBuyer().getEmail();
        String buyerName      = visitRequest.getBuyer().getName();
        String propertyTitle  = visitRequest.getProperty().getTitle();
        LocalDateTime scheduledAt = visit.getScheduledAt();
        afterCommit(() -> emailService.sendVisitRequestAcceptedEmail(
                buyerEmail, buyerName, propertyTitle, scheduledAt));

        return toResponse(visitRequestRepository.save(visitRequest));
    }

    // ─── REJECT (agent) ───────────────────────────────────────────

    public VisitRequestResponse reject(Long visitRequestId, String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        VisitRequest visitRequest = getVisitRequest(visitRequestId);

        validateIsAssignedAgent(visitRequest, currentUser);
        validatePendingOrCounterProposed(visitRequest);

        visitRequest.setStatus(VisitRequestStatus.REJECTED);
        String buyerEmail    = visitRequest.getBuyer().getEmail();
        String buyerName     = visitRequest.getBuyer().getName();
        String propertyTitle = visitRequest.getProperty().getTitle();
        afterCommit(() -> emailService.sendVisitRequestRejectedEmail(buyerEmail, buyerName, propertyTitle));
        return toResponse(visitRequestRepository.save(visitRequest));
    }

    // ─── COUNTER PROPOSE (agent) ──────────────────────────────────

    public VisitRequestResponse counterPropose(Long visitRequestId, CounterProposeRequest request,
            String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        VisitRequest visitRequest = getVisitRequest(visitRequestId);

        validateIsAssignedAgent(visitRequest, currentUser);
        validatePendingOrCounterProposed(visitRequest);

        visitRequest.setCounterProposedAt(request.counterProposedAt());
        visitRequest.setCounterProposeMessage(request.counterProposeMessage());
        visitRequest.setStatus(VisitRequestStatus.COUNTER_PROPOSED);
        String buyerEmail    = visitRequest.getBuyer().getEmail();
        String buyerName     = visitRequest.getBuyer().getName();
        String propertyTitle = visitRequest.getProperty().getTitle();
        LocalDateTime counterAt  = request.counterProposedAt();
        String counterMsg        = request.counterProposeMessage();
        afterCommit(() -> emailService.sendVisitCounterProposedEmail(
                buyerEmail, buyerName, propertyTitle, counterAt, counterMsg));

        return toResponse(visitRequestRepository.save(visitRequest));
    }

    // ─── CANCEL (buyer) ───────────────────────────────────────────

    public VisitRequestResponse cancel(Long visitRequestId, String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        VisitRequest visitRequest = getVisitRequest(visitRequestId);

        if (!visitRequest.getBuyer().getId().equals(currentUser.getId())
                && currentUser.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Solo el comprador puede cancelar su solicitud de visita");
        }

        if (visitRequest.getStatus() == VisitRequestStatus.ACCEPTED
                || visitRequest.getStatus() == VisitRequestStatus.REJECTED
                || visitRequest.getStatus() == VisitRequestStatus.CANCELLED) {
            throw new BadRequestException(
                    "No se puede cancelar una solicitud en estado: " + visitRequest.getStatus());
        }

        visitRequest.setStatus(VisitRequestStatus.CANCELLED);
        if (visitRequest.getAgent() != null) {
            String agentEmail    = visitRequest.getAgent().getUser().getEmail();
            String agentName     = visitRequest.getAgent().getUser().getName();
            String propertyTitle = visitRequest.getProperty().getTitle();
            String buyerName     = visitRequest.getBuyerName();
            afterCommit(() -> emailService.sendVisitRequestCancelledEmail(
                    agentEmail, agentName, propertyTitle, buyerName));
        }
        return toResponse(visitRequestRepository.save(visitRequest));
    }

    // ─── QUERIES ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<VisitRequestResponse> getMyRequestsAsBuyer(String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        return visitRequestRepository.findByBuyerId(currentUser.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VisitRequestResponse> getMyRequestsAsAgent(String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);

        if (currentUser.getRole() == UserRole.ADMIN) {
            return visitRequestRepository.findAll()
                    .stream().map(this::toResponse).collect(Collectors.toList());
        }

        Optional<AgentProfile> agentProfileOpt = agentProfileRepository.findByUser_Id(currentUser.getId());
        if (agentProfileOpt.isEmpty()) {
            return List.of(); // Devuelve lista vacía en lugar de error 400 si el agente no tiene perfil aún
        }

        return visitRequestRepository.findByAgentId(agentProfileOpt.get().getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VisitRequestResponse> getByProperty(Long propertyId, String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        Property property = getProperty(propertyId);

        boolean isOwner = property.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isPropertyAgent = property.getAgent() != null
                && agentProfileRepository.findByUser_Id(currentUser.getId())
                        .map(a -> a.getId().equals(property.getAgent().getId()))
                        .orElse(false);

        if (!isOwner && !isAdmin && !isPropertyAgent) {
            throw new BadRequestException(
                    "No tienes permiso para ver las solicitudes de visita de esta propiedad");
        }

        return visitRequestRepository.findByPropertyId(propertyId)
                .stream().map(this::toResponse).collect(Collectors.toList());
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

    private VisitRequest getVisitRequest(Long id) {
        return visitRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Solicitud de visita no encontrada con ID: " + id));
    }

    private AgentClient ensureAgentClientExists(AgentProfile agent, User buyer) {
        return agentClientRepository.findByAgent_IdAndUser_Id(agent.getId(), buyer.getId())
                .orElseGet(() -> agentClientRepository.save(AgentClient.builder()
                        .agent(agent)
                        .user(buyer)
                        .build()));
    }

    private void validateIsAssignedAgent(VisitRequest visitRequest, User currentUser) {
        if (currentUser.getRole() == UserRole.ADMIN) return;

        AgentProfile myProfile = agentProfileRepository.findByUser_Id(currentUser.getId())
                .orElseThrow(() -> new BadRequestException(
                        "No tienes un perfil de agente asociado a tu cuenta"));

        if (visitRequest.getAgent() == null
                || !visitRequest.getAgent().getId().equals(myProfile.getId())) {
            throw new BadRequestException("No eres el agente asignado a esta solicitud de visita");
        }
    }

    private void validatePendingOrCounterProposed(VisitRequest visitRequest) {
        if (visitRequest.getStatus() != VisitRequestStatus.PENDING
                && visitRequest.getStatus() != VisitRequestStatus.COUNTER_PROPOSED) {
            throw new BadRequestException(
                    "La solicitud debe estar en estado PENDING o COUNTER_PROPOSED. Estado actual: "
                            + visitRequest.getStatus());
        }
    }

    private AgentProfile resolveAssignedAgent(Property property) {
        if (property.getAgent() != null) {
            return property.getAgent();
        }

        return propertyAssignmentRepository
                .findTopByProperty_IdAndStatusOrderByAssignedAtDesc(property.getId(), AssignmentStatus.ACCEPTED)
                .map(pa -> pa.getAgent())
                .orElse(null);
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private VisitRequestResponse toResponse(VisitRequest vr) {
        return new VisitRequestResponse(
                vr.getId(),
                vr.getProperty().getId(),
                vr.getProperty().getTitle(),
                vr.getBuyer().getId(),
                vr.getBuyerName(),
                vr.getBuyerEmail(),
                vr.getBuyerPhone(),
                vr.getAgent() != null ? vr.getAgent().getId() : null,
                vr.getAgent() != null ? vr.getAgent().getUser().getName() : null,
                vr.getProposedAt(),
                vr.getCounterProposedAt(),
                vr.getCounterProposeMessage(),
                vr.getStatus(),
                vr.getMessage(),
                vr.getVisit() != null ? vr.getVisit().getId() : null,
                vr.getCreatedAt(),
                vr.getUpdatedAt());
    }
}
