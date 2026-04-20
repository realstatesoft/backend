package com.openroof.openroof.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openroof.openroof.dto.offer.OfferRequestDTO;
import com.openroof.openroof.dto.offer.OfferResponseDTO;
import com.openroof.openroof.dto.offer.UpdateOfferStatusDTO;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.OfferStatus;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.interaction.Offer;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.OfferRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OfferService {

    private final OfferRepository offerRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final AgentProfileRepository agentProfileRepository;

    public OfferResponseDTO createOffer(OfferRequestDTO request, String currentUserEmail) {
        User buyer = getUserByEmail(currentUserEmail);
        Property property = getProperty(request.getPropertyId());

        if (property.getOwner().getId().equals(buyer.getId())) {
            throw new BadRequestException("No puedes realizar una oferta sobre tu propia propiedad");
        }

        // Bloquear al agente asignado
        if (property.getAgent() != null) {
            AgentProfile agent = agentProfileRepository.findByUser_Id(buyer.getId()).orElse(null);
            if (agent != null && property.getAgent().getId().equals(agent.getId())) {
                throw new BadRequestException("Como agente asignado a esta propiedad, no puedes realizar ofertas sobre ella");
            }
        }

        Offer offer = Offer.builder()
                .property(property)
                .buyer(buyer)
                .amount(request.getAmount())
                .message(request.getMessage())
                .status(OfferStatus.SENT)
                .build();

        return toResponseDTO(offerRepository.save(offer));
    }

    @Transactional(readOnly = true)
    public Page<OfferResponseDTO> getMyOffersAsBuyer(String currentUserEmail, Pageable pageable) {
        User buyer = getUserByEmail(currentUserEmail);
        return offerRepository.findByBuyer_Id(buyer.getId(), pageable)
                .map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public Page<OfferResponseDTO> getReceivedOffers(String currentUserEmail, Pageable pageable) {
        User user = getUserByEmail(currentUserEmail);
        
        if (user.getRole() == UserRole.ADMIN) {
            return offerRepository.findAll(pageable)
                    .map(this::toResponseDTO);
        }

        // Obtenemos el ID de agente si existe
        Long agentId = agentProfileRepository.findByUser_Id(user.getId())
                .map(AgentProfile::getId)
                .orElse(-1L); // -1 if not an agent

        return offerRepository.findReceivedOffers(user.getId(), agentId, pageable)
                .map(this::toResponseDTO);
    }

    public OfferResponseDTO updateOfferStatus(Long offerId, UpdateOfferStatusDTO request, String currentUserEmail) {
        User user = getUserByEmail(currentUserEmail);
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta no encontrada"));

        validateManagementPermission(offer, user);
        
        try {
            // Validar transición de estado
            offer.setStatus(offer.getStatus().transitionTo(request.getStatus()));
        } catch (IllegalStateException e) {
            throw new BadRequestException("Transición de estado inválida: " + e.getMessage());
        }
        
        if (request.getStatus() == OfferStatus.REJECTED) {
            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw new BadRequestException("Debe proporcionar un motivo de rechazo");
            }
            offer.setRejectionReason(request.getRejectionReason());
        }
        
        if (request.getStatus() == OfferStatus.NEGOTIATING) {
            if (request.getCounterOfferAmount() == null) {
                throw new BadRequestException("Debe proporcionar el monto de la contraoferta");
            }
            offer.setCounterOfferAmount(request.getCounterOfferAmount());
        } else {
            // Ignorar el monto si no es una contraoferta
            offer.setCounterOfferAmount(null);
        }

        return toResponseDTO(offerRepository.save(offer));
    }

    @Transactional(readOnly = true)
    public List<OfferResponseDTO> getOffersByProperty(Long propertyId, String currentUserEmail) {
        User user = getUserByEmail(currentUserEmail);
        Property property = getProperty(propertyId);
        
        validatePropertyAccess(property, user);

        return offerRepository.findByProperty_Id(propertyId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public OfferResponseDTO updateOffer(Long offerId, OfferRequestDTO request, String currentUserEmail) {
        User user = getUserByEmail(currentUserEmail);
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta no encontrada"));

        if (!offer.getBuyer().getId().equals(user.getId())) {
            throw new ForbiddenException("No puedes editar una oferta que no realizaste");
        }

        if (offer.getStatus() != OfferStatus.SENT && offer.getStatus() != OfferStatus.VIEWED) {
            throw new BadRequestException("No puedes editar una oferta que ya ha sido procesada");
        }

        offer.setAmount(request.getAmount());
        offer.setMessage(request.getMessage());
        offer.setStatus(OfferStatus.SENT); // Volver a enviada al ser modificada
        
        // Limpiar campos de negociación previos
        offer.setRejectionReason(null);
        offer.setCounterOfferAmount(null);
        
        return toResponseDTO(offerRepository.save(offer));
    }

    // ─── HELPERS ──────────────────────────────────────────────────

    private void validateManagementPermission(Offer offer, User user) {
        hasPropertyManagementPermission(offer.getProperty(), user, "No tienes permiso para gestionar esta oferta");
    }

    private void validatePropertyAccess(Property property, User user) {
        hasPropertyManagementPermission(property, user, "No tienes permiso para ver las ofertas de esta propiedad");
    }

    private void hasPropertyManagementPermission(Property property, User user, String errorMessage) {
        if (user.getRole() == UserRole.ADMIN) return;

        boolean isOwner = property.getOwner().getId().equals(user.getId());
        boolean isAgent = false;
        
        if (property.getAgent() != null) {
            isAgent = agentProfileRepository.findByUser_Id(user.getId())
                    .map(a -> a.getId().equals(property.getAgent().getId()))
                    .orElse(false);
        }

        if (!isOwner && !isAgent) {
            throw new ForbiddenException(errorMessage);
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Property getProperty(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));
    }

    private OfferResponseDTO toResponseDTO(Offer offer) {
        return OfferResponseDTO.builder()
                .id(offer.getId())
                .propertyId(offer.getProperty().getId())
                .propertyTitle(offer.getProperty().getTitle())
                .buyerId(offer.getBuyer().getId())
                .buyerName(offer.getBuyer().getName())
                .buyerEmail(offer.getBuyer().getEmail())
                .buyerPhone(offer.getBuyer().getPhone())
                .amount(offer.getAmount())
                .status(offer.getStatus())
                .message(offer.getMessage())
                .rejectionReason(offer.getRejectionReason())
                .counterOfferAmount(offer.getCounterOfferAmount())
                .createdAt(offer.getCreatedAt())
                .build();
    }
}
