package com.openroof.openroof.service;

import com.openroof.openroof.dto.contract.*;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.ContractMapper;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.contract.Contract;
import com.openroof.openroof.model.contract.ContractTemplate;
import com.openroof.openroof.model.enums.ContractStatus;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final PropertyRepository propertyRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final ContractMapper contractMapper;

    // ─── CREATE ───────────────────────────────────────────────────────────────

    /**
     * Crea un contrato aplicando las reglas de comisión según el escenario:
     *
     * A) Directo (sin agentes)  → commissionPct debe ser 0
     * B) Solo agente listador   → listingAgentId presente, buyerAgentId null
     * C) Solo agente comprador  → buyerAgentId presente, listingAgentId null
     * D) Dual agency            → ambos agentes presentes, pcts suman commissionPct
     */
    @Transactional
    public ContractResponse create(ContractRequest request) {
        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));

        User buyer = userRepository.findById(request.buyerId())
                .orElseThrow(() -> new ResourceNotFoundException("Comprador/inquilino no encontrado"));

        User seller = userRepository.findById(request.sellerId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor/propietario no encontrado"));

        AgentProfile listingAgent = resolveAgent(request.listingAgentId(), "Agente listador no encontrado");
        AgentProfile buyerAgent   = resolveAgent(request.buyerAgentId(),   "Agente del comprador no encontrado");

        BigDecimal commissionPct        = coerceZero(request.commissionPct());
        BigDecimal listingAgentPct      = coerceZero(request.listingAgentCommissionPct());
        BigDecimal buyerAgentPct        = coerceZero(request.buyerAgentCommissionPct());

        validateCommissionRules(listingAgent, buyerAgent, commissionPct, listingAgentPct, buyerAgentPct);

        ContractTemplate template = null;
        if (request.templateId() != null) {
            template = contractTemplateRepository.findById(request.templateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada"));
        }

        Contract contract = Contract.builder()
                .property(property)
                .buyer(buyer)
                .seller(seller)
                .listingAgent(listingAgent)
                .buyerAgent(buyerAgent)
                .template(template)
                .contractType(request.contractType())
                .amount(request.amount())
                .commissionPct(commissionPct)
                .listingAgentCommissionPct(listingAgentPct)
                .buyerAgentCommissionPct(buyerAgentPct)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .terms(request.terms())
                .build();

        return contractMapper.toResponse(contractRepository.save(contract));
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    /**
     * Obtiene un contrato por ID. Solo puede acceder quien es parte del contrato
     * (comprador, vendedor, agente listador, agente comprador) o un ADMIN.
     */
    public ContractResponse getById(Long id, String requesterEmail) {
        Contract contract = findOrThrow(id);
        User requester = findUserByEmail(requesterEmail);

        if (!canAccess(contract, requester)) {
            throw new BadRequestException("No tiene permiso para ver este contrato");
        }

        return contractMapper.toResponse(contract);
    }

    /** Contratos donde el agente autenticado actúa como agente listador. */
    public List<ContractSummaryResponse> getAsListingAgent(String agentEmail) {
        AgentProfile agent = findAgentByEmail(agentEmail);
        return contractRepository.findByListingAgent_Id(agent.getId()).stream()
                .map(contractMapper::toSummaryResponse)
                .toList();
    }

    /** Contratos donde el agente autenticado actúa como agente del comprador. */
    public List<ContractSummaryResponse> getAsBuyerAgent(String agentEmail) {
        AgentProfile agent = findAgentByEmail(agentEmail);
        return contractRepository.findByBuyerAgent_Id(agent.getId()).stream()
                .map(contractMapper::toSummaryResponse)
                .toList();
    }

    /** Contratos donde el usuario autenticado es vendedor/propietario. */
    public List<ContractSummaryResponse> getAsSeller(String sellerEmail) {
        User user = findUserByEmail(sellerEmail);
        return contractRepository.findBySeller_Id(user.getId()).stream()
                .map(contractMapper::toSummaryResponse)
                .toList();
    }

    /** Contratos donde el usuario autenticado es comprador/inquilino. */
    public List<ContractSummaryResponse> getAsBuyer(String buyerEmail) {
        User user = findUserByEmail(buyerEmail);
        return contractRepository.findByBuyer_Id(user.getId()).stream()
                .map(contractMapper::toSummaryResponse)
                .toList();
    }

    /** Todos los contratos de una propiedad si el usuario participa en alguno de ellos o es ADMIN. */
    public List<ContractSummaryResponse> getByProperty(Long propertyId, String requesterEmail) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new ResourceNotFoundException("Propiedad no encontrada");
        }

        User requester = findUserByEmail(requesterEmail);
        List<Contract> contracts = contractRepository.findByProperty_Id(propertyId);

        if (requester.getRole() != UserRole.ADMIN && contracts.stream().noneMatch(contract -> canAccess(contract, requester))) {
            throw new BadRequestException("No tiene permiso para ver los contratos de esta propiedad");
        }

        return contracts.stream()
                .map(contractMapper::toSummaryResponse)
                .toList();
    }

    // ─── UPDATE STATUS ────────────────────────────────────────────────────────

    @Transactional
    public ContractResponse updateStatus(Long id, ContractStatusUpdateRequest request, String requesterEmail) {
        Contract contract = findOrThrow(id);
        User requester = findUserByEmail(requesterEmail);

        if (!canAccess(contract, requester)) {
            throw new BadRequestException("No tiene permiso para modificar este contrato");
        }

        validateStatusTransition(contract.getStatus(), request.status(), requester);

        contract.setStatus(request.status());
        return contractMapper.toResponse(contractRepository.save(contract));
    }

    // ─── DELETE (soft) ────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id, String requesterEmail) {
        Contract contract = findOrThrow(id);
        User requester = findUserByEmail(requesterEmail);

        if (!canAccess(contract, requester)) {
            throw new BadRequestException("No tiene permiso para modificar este contrato");
        }

        contract.setDeletedAt(LocalDateTime.now());
        contractRepository.save(contract);
    }

    // ─── Validaciones de comisión ─────────────────────────────────────────────

    /**
     * Aplica las reglas de negocio sobre los porcentajes de comisión:
     *
     * 1. commissionPct == listingAgentPct + buyerAgentPct
     * 2. Si no hay agente listador, listingAgentPct debe ser 0
     * 3. Si no hay agente comprador, buyerAgentPct debe ser 0
     * 4. Si no hay ningún agente, commissionPct debe ser 0
     */
    private void validateCommissionRules(AgentProfile listingAgent, AgentProfile buyerAgent,
                                         BigDecimal commissionPct, BigDecimal listingPct, BigDecimal buyerPct) {
        if (listingAgent == null && listingPct.compareTo(BigDecimal.ZERO) != 0) {
            throw new BadRequestException(
                    "El porcentaje del agente listador debe ser 0 cuando no hay agente listador");
        }
        if (buyerAgent == null && buyerPct.compareTo(BigDecimal.ZERO) != 0) {
            throw new BadRequestException(
                    "El porcentaje del agente comprador debe ser 0 cuando no hay agente del comprador");
        }
        if (listingAgent == null && buyerAgent == null && commissionPct.compareTo(BigDecimal.ZERO) != 0) {
            throw new BadRequestException(
                    "La comisión debe ser 0 en contratos directos sin agentes intermediarios");
        }

        BigDecimal sumPct = listingPct.add(buyerPct);
        // Usar comparación con escala para evitar problemas de redondeo (tolerancia: 0.01%)
        if (commissionPct.subtract(sumPct).abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
            throw new BadRequestException(
                    "commissionPct (" + commissionPct + "%) debe ser igual a la suma de " +
                    "listingAgentCommissionPct (" + listingPct + "%) + " +
                    "buyerAgentCommissionPct (" + buyerPct + "%)");
        }
    }

    /**
     * Valida que la transición de estado sea permitida.
     * Los ADMINs pueden hacer cualquier transición.
     * Los AGENTs pueden avanzar el flujo normal pero no pueden cancelar contratos firmados.
     */
    private void validateStatusTransition(ContractStatus current, ContractStatus next, User requester) {
        if (requester.getRole() == UserRole.ADMIN) {
            return;
        }
        boolean valid = switch (current) {
            case DRAFT            -> next == ContractStatus.SENT || next == ContractStatus.CANCELLED;
            case SENT             -> next == ContractStatus.PARTIALLY_SIGNED
                                  || next == ContractStatus.REJECTED
                                  || next == ContractStatus.CANCELLED;
            case PARTIALLY_SIGNED -> next == ContractStatus.SIGNED
                                  || next == ContractStatus.REJECTED
                                  || next == ContractStatus.CANCELLED;
            default               -> false; // SIGNED, REJECTED, EXPIRED, CANCELLED son terminales
        };
        if (!valid) {
            throw new BadRequestException(
                    "Transición de estado no permitida: " + current + " → " + next);
        }
    }

    // ─── Control de acceso ────────────────────────────────────────────────────

    private boolean canAccess(Contract contract, User requester) {
        if (requester.getRole() == UserRole.ADMIN) return true;

        Long uid = requester.getId();
        if (uid.equals(safeId(contract.getBuyer())))  return true;
        if (uid.equals(safeId(contract.getSeller()))) return true;

        if (requester.getRole() == UserRole.AGENT) {
            AgentProfile agentProfile = agentProfileRepository.findByUser_Id(uid).orElse(null);
            if (agentProfile != null) {
                Long aid = agentProfile.getId();
                if (aid.equals(safeId(contract.getListingAgent()))) return true;
                if (aid.equals(safeId(contract.getBuyerAgent())))   return true;
            }
        }
        return false;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Contract findOrThrow(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato no encontrado"));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private AgentProfile findAgentByEmail(String email) {
        User user = findUserByEmail(email);
        return agentProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de agente no encontrado"));
    }

    private AgentProfile resolveAgent(Long agentId, String errorMessage) {
        if (agentId == null) return null;
        return agentProfileRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException(errorMessage));
    }

    private BigDecimal coerceZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Long safeId(Object entity) {
        if (entity instanceof User u)         return u.getId();
        if (entity instanceof AgentProfile a) return a.getId();
        return null;
    }
}
