package com.openroof.openroof.service;

import com.openroof.openroof.dto.contract.*;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.ContractMapper;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.contract.Contract;
import com.openroof.openroof.model.contract.ContractSignature;
import com.openroof.openroof.model.contract.ContractTemplate;
import com.openroof.openroof.model.enums.ContractStatus;
import com.openroof.openroof.model.enums.SignatureRole;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.ContractRepository;
import com.openroof.openroof.repository.ContractSignatureRepository;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.ContractTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.openroof.openroof.model.enums.AuditAction;
import com.openroof.openroof.model.enums.AuditEntityType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ContractService {

    private static final BigDecimal MAX_PERCENTAGE = BigDecimal.valueOf(100);
    private static final BigDecimal PERCENTAGE_TOLERANCE = BigDecimal.valueOf(0.01);

    private final ContractRepository contractRepository;
    private final ContractSignatureRepository contractSignatureRepository;
    private final UserRepository userRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final PropertyRepository propertyRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final ContractMapper contractMapper;
    private final EmailService emailService;
    private final AuditService auditService;

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
    public ContractResponse create(ContractRequest request, String requesterEmail) {
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

        User requester = findUserByEmail(requesterEmail);
        if (!canManageContract(contract, requester)) {
            throw new BadRequestException("No tiene permiso para crear un contrato con estos participantes");
        }

        contractRepository.save(contract);
        ContractResponse saved = contractMapper.toResponse(contract);
        auditService.log(requester, AuditEntityType.CONTRACT, contract.getId(), AuditAction.CREATE, null,
                contractAuditSnapshot(contract));

        String propertyTitle = property.getTitle();
        emailService.sendContractCreatedEmail(buyer.getEmail(), buyer.getName(), propertyTitle, saved.id());
        emailService.sendContractCreatedEmail(seller.getEmail(), seller.getName(), propertyTitle, saved.id());
        if (listingAgent != null) {
            emailService.sendContractCreatedEmail(listingAgent.getUser().getEmail(),
                    listingAgent.getUser().getName(), propertyTitle, saved.id());
        }
        if (buyerAgent != null && (listingAgent == null || !buyerAgent.getId().equals(listingAgent.getId()))) {
            emailService.sendContractCreatedEmail(buyerAgent.getUser().getEmail(),
                    buyerAgent.getUser().getName(), propertyTitle, saved.id());
        }
        return saved;
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    /**
     * Actualiza un contrato existente. Solo se permite si el contrato está en
     * estado DRAFT y el solicitante tiene permisos.
     */
    @Transactional
    public ContractResponse update(Long id, ContractRequest request, String requesterEmail) {
        Contract contract = findOrThrow(id);
        User requester = findUserByEmail(requesterEmail);

        if (!canManageContract(contract, requester)) {
            throw new BadRequestException("No tiene permiso para editar este contrato");
        }

        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new BadRequestException("Solo se pueden editar contratos en estado DRAFT");
        }

        // 1. Resolver nuevas entidades
        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));
        User buyer = userRepository.findById(request.buyerId())
                .orElseThrow(() -> new ResourceNotFoundException("Comprador no encontrado"));
        User seller = userRepository.findById(request.sellerId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor no encontrado"));
        AgentProfile listingAgent = resolveAgent(request.listingAgentId(), "Agente listador no encontrado");
        AgentProfile buyerAgent   = resolveAgent(request.buyerAgentId(),   "Agente del comprador no encontrado");

        // 2. Validar reglas de comisión
        BigDecimal commissionPct   = coerceZero(request.commissionPct());
        BigDecimal listingAgentPct = coerceZero(request.listingAgentCommissionPct());
        BigDecimal buyerAgentPct   = coerceZero(request.buyerAgentCommissionPct());
        validateCommissionRules(listingAgent, buyerAgent, commissionPct, listingAgentPct, buyerAgentPct);

        // 3. Actualizar campos
        contract.setProperty(property);
        contract.setBuyer(buyer);
        contract.setSeller(seller);
        contract.setListingAgent(listingAgent);
        contract.setBuyerAgent(buyerAgent);
        contract.setContractType(request.contractType());
        contract.setAmount(request.amount());
        contract.setCommissionPct(commissionPct);
        contract.setListingAgentCommissionPct(listingAgentPct);
        contract.setBuyerAgentCommissionPct(buyerAgentPct);
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setTerms(request.terms());

        if (request.templateId() != null) {
            ContractTemplate template = contractTemplateRepository.findById(request.templateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada"));
            contract.setTemplate(template);
        }

        return contractMapper.toResponse(contractRepository.save(contract));
    }

    // ─── SIGNATURES ──────────────────────────────────────────────────────────

    /**
     * Registra una firma digital para el contrato.
     */
    @Transactional
    public ContractResponse sign(Long id, SignContractRequest request, String email, String ip) {
        Contract contract = contractRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato no encontrado"));
        User user = findUserByEmail(email);

        // Capturar estado actual para detectar cambios
        ContractStatus oldStatus = contract.getStatus();

        // 1. Validar estado
        if (contract.getStatus() != ContractStatus.SENT && contract.getStatus() != ContractStatus.PARTIALLY_SIGNED) {
            throw new BadRequestException("El contrato no está en un estado que permita firmas");
        }

        // 2. Validar que el usuario sea la parte que dice ser y resolver rol específico
        validateSignerIdentity(contract, user, request.role());
        
        SignatureRole finalizedRole = request.role();
        if (finalizedRole == SignatureRole.AGENT) {
            if (contract.getListingAgent() != null && user.getId().equals(contract.getListingAgent().getUser().getId())) {
                finalizedRole = SignatureRole.LISTING_AGENT;
            } else if (contract.getBuyerAgent() != null && user.getId().equals(contract.getBuyerAgent().getUser().getId())) {
                finalizedRole = SignatureRole.BUYER_AGENT;
            }
        }

        // 3. Validar que no haya firmado ya para ese rol específico
        if (contractSignatureRepository.existsByContractIdAndRoleAndDeletedAtIsNull(id, finalizedRole)) {
            throw new BadRequestException("Esta parte ya ha firmado el contrato");
        }

        // 4. Registrar firma
        ContractSignature signature = ContractSignature.builder()
                .contract(contract)
                .signer(user)
                .role(finalizedRole)
                .signatureType(request.signatureType())
                .signatureData(request.signatureData())
                .ipAddress(ip)
                .signedAt(LocalDateTime.now())
                .build();
        contractSignatureRepository.save(signature);

        // 5. Evaluar cambio de estado
        evaluateCompletion(contract);

        Contract saved = contractRepository.save(contract);

        // 6. Notificar si el estado cambió
        if (saved.getStatus() != oldStatus) {
            String propertyTitle = saved.getProperty().getTitle();
            String newStatus = saved.getStatus().name();
            String buyerEmail      = saved.getBuyer().getEmail();
            String buyerName       = saved.getBuyer().getName();
            String sellerEmail     = saved.getSeller().getEmail();
            String sellerName      = saved.getSeller().getName();
            AgentProfile la = saved.getListingAgent();
            AgentProfile ba = saved.getBuyerAgent();
            String listingAgentEmail = la != null ? la.getUser().getEmail() : null;
            String listingAgentName  = la != null ? la.getUser().getName()  : null;
            String buyerAgentEmail   = ba != null ? ba.getUser().getEmail() : null;
            String buyerAgentName    = ba != null ? ba.getUser().getName()  : null;
            boolean sameAgent = la != null && ba != null && la.getId().equals(ba.getId());

            afterCommit(() -> {
                emailService.sendContractStatusChangedEmail(buyerEmail,  buyerName,  propertyTitle, newStatus, saved.getId());
                emailService.sendContractStatusChangedEmail(sellerEmail, sellerName, propertyTitle, newStatus, saved.getId());
                if (listingAgentEmail != null) {
                    emailService.sendContractStatusChangedEmail(listingAgentEmail, listingAgentName, propertyTitle, newStatus, saved.getId());
                }
                if (buyerAgentEmail != null && !sameAgent) {
                    emailService.sendContractStatusChangedEmail(buyerAgentEmail, buyerAgentName, propertyTitle, newStatus, saved.getId());
                }
            });
        }

        return contractMapper.toResponse(saved);
    }

    public List<SignatureStatusResponse> getSignatures(Long id, String requesterEmail) {
        Contract contract = findOrThrow(id);
        User requester = findUserByEmail(requesterEmail);

        if (!canAccess(contract, requester)) {
            throw new BadRequestException("No tiene permiso para ver el estado de firmas de este contrato");
        }

        List<ContractSignature> existingSigs = contractSignatureRepository.findByContractIdAndDeletedAtIsNull(id);
        
        // Mapeo rápido para búsqueda
        var signedMap = existingSigs.stream()
                .collect(Collectors.toMap(ContractSignature::getRole, s -> s));

        // Construir lista de todos los que DEBEN firmar
        List<SignatureStatusResponse> results = new java.util.ArrayList<>();

        // 1. Vendedor
        results.add(mapSignatureStatus(SignatureRole.SELLER, contract.getSeller(), signedMap.get(SignatureRole.SELLER)));
        
        // 2. Comprador
        results.add(mapSignatureStatus(SignatureRole.BUYER, contract.getBuyer(), signedMap.get(SignatureRole.BUYER)));
        
        // 3. Agente Listador (si existe)
        if (contract.getListingAgent() != null) {
            results.add(mapSignatureStatus(SignatureRole.LISTING_AGENT, contract.getListingAgent().getUser(), signedMap.get(SignatureRole.LISTING_AGENT)));
        }
        
        // 4. Agente Comprador (si existe)
        if (contract.getBuyerAgent() != null) {
            results.add(mapSignatureStatus(SignatureRole.BUYER_AGENT, contract.getBuyerAgent().getUser(), signedMap.get(SignatureRole.BUYER_AGENT)));
        }

        return results;
    }

    private SignatureStatusResponse mapSignatureStatus(SignatureRole role, User user, ContractSignature sig) {
        return new SignatureStatusResponse(
                sig != null ? sig.getId() : null,
                user.getId(),
                user.getName(),
                user.getEmail(),
                role,
                sig != null ? sig.getSignatureType() : null,
                sig != null ? sig.getSignedAt() : null,
                sig != null
        );
    }

    private void validateSignerIdentity(Contract contract, User user, SignatureRole role) {
        boolean valid = false;
        Long userId = user.getId();
        
        log.debug("Validando firma - UserID: {}, Role: {}, ContractID: {}", userId, role, contract.getId());

        switch (role) {
            case BUYER -> valid = userId.equals(contract.getBuyer().getId());
            case SELLER -> valid = userId.equals(contract.getSeller().getId());
            case LISTING_AGENT -> {
                if (contract.getListingAgent() == null) break;
                valid = userId.equals(contract.getListingAgent().getUser().getId());
            }
            case BUYER_AGENT -> {
                if (contract.getBuyerAgent() == null) break;
                valid = userId.equals(contract.getBuyerAgent().getUser().getId());
            }
            case AGENT -> {
                boolean isListing = contract.getListingAgent() != null && 
                                   userId.equals(contract.getListingAgent().getUser().getId());
                boolean isBuyer = contract.getBuyerAgent() != null && 
                                 userId.equals(contract.getBuyerAgent().getUser().getId());
                valid = isListing || isBuyer;
            }
            default -> valid = false;
        }

        if (!valid) {
            log.warn("Falla validación de identidad para contrato {} - Usuario {} no corresponde al rol {}", 
                    contract.getId(), userId, role);
            throw new BadRequestException("El usuario no corresponde al rol de firma seleccionado");
        }
    }

    private void evaluateCompletion(Contract contract) {
        List<ContractSignature> sigs = contractSignatureRepository.findByContractIdAndDeletedAtIsNull(contract.getId());
        Set<SignatureRole> signedRoles = sigs.stream().map(ContractSignature::getRole).collect(Collectors.toSet());

        boolean buyerSigned = signedRoles.contains(SignatureRole.BUYER);
        boolean sellerSigned = signedRoles.contains(SignatureRole.SELLER);
        boolean listingAgentSigned = contract.getListingAgent() == null || signedRoles.contains(SignatureRole.LISTING_AGENT);
        boolean buyerAgentSigned = contract.getBuyerAgent() == null || signedRoles.contains(SignatureRole.BUYER_AGENT);

        if (buyerSigned && sellerSigned && listingAgentSigned && buyerAgentSigned) {
            contract.setStatus(ContractStatus.SIGNED);
        } else {
            contract.setStatus(ContractStatus.PARTIALLY_SIGNED);
        }
    }

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
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));
        User requester = findUserByEmail(requesterEmail);
        if (!canManagePropertyContracts(property, requester)) {
            throw new BadRequestException("No tiene permiso para ver los contratos de esta propiedad");
        }

        List<Contract> contracts = contractRepository.findByProperty_Id(propertyId);
        return contracts.stream()
                .map(contractMapper::toSummaryResponse)
                .toList();
    }

    // ─── UPDATE STATUS ────────────────────────────────────────────────────────

    @Transactional
    public ContractResponse updateStatus(Long id, ContractStatusUpdateRequest request, String requesterEmail) {
        Contract contract = findOrThrow(id);
        User requester = findUserByEmail(requesterEmail);

        if (!canManageContract(contract, requester)) {
            throw new BadRequestException("No tiene permiso para modificar este contrato");
        }

        ContractStatus previousStatus = contract.getStatus();
        validateStatusTransition(previousStatus, request.status(), requester);

        contract.setStatus(request.status());
        ContractResponse response = contractMapper.toResponse(contractRepository.save(contract));

        auditService.log(requester, AuditEntityType.CONTRACT, id, AuditAction.STATUS_CHANGE,
                Map.of("status", previousStatus.name()),
                Map.of("status", request.status().name()));

        String propertyTitle = contract.getProperty().getTitle();
        String newStatus = request.status().name();
        String buyerEmail      = contract.getBuyer().getEmail();
        String buyerName       = contract.getBuyer().getName();
        String sellerEmail     = contract.getSeller().getEmail();
        String sellerName      = contract.getSeller().getName();
        AgentProfile la = contract.getListingAgent();
        AgentProfile ba = contract.getBuyerAgent();
        String listingAgentEmail = la != null ? la.getUser().getEmail() : null;
        String listingAgentName  = la != null ? la.getUser().getName()  : null;
        String buyerAgentEmail   = ba != null ? ba.getUser().getEmail() : null;
        String buyerAgentName    = ba != null ? ba.getUser().getName()  : null;
        boolean sameAgent = la != null && ba != null && la.getId().equals(ba.getId());

        afterCommit(() -> {
            emailService.sendContractStatusChangedEmail(buyerEmail,  buyerName,  propertyTitle, newStatus, contract.getId());
            emailService.sendContractStatusChangedEmail(sellerEmail, sellerName, propertyTitle, newStatus, contract.getId());
            if (listingAgentEmail != null) {
                emailService.sendContractStatusChangedEmail(listingAgentEmail, listingAgentName, propertyTitle, newStatus, contract.getId());
            }
            if (buyerAgentEmail != null && !sameAgent) {
                emailService.sendContractStatusChangedEmail(buyerAgentEmail, buyerAgentName, propertyTitle, newStatus, contract.getId());
            }
        });

        return response;
    }

    // ─── DELETE (soft) ────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id, String requesterEmail) {
        Contract contract = findOrThrow(id);
        User requester = findUserByEmail(requesterEmail);

        if (!canManageContract(contract, requester)) {
            throw new BadRequestException("No tiene permiso para modificar este contrato");
        }

        Map<String, Object> before = contractAuditSnapshot(contract);
        contract.setDeletedAt(LocalDateTime.now());
        contractRepository.save(contract);
        auditService.log(requester, AuditEntityType.CONTRACT, id, AuditAction.DELETE, before,
                Map.of("deletedAt", contract.getDeletedAt().toString()));
    }

    private Map<String, Object> contractAuditSnapshot(Contract contract) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", contract.getId());
        m.put("status", contract.getStatus() != null ? contract.getStatus().name() : null);
        m.put("propertyId", contract.getProperty() != null ? contract.getProperty().getId() : null);
        m.put("amount", contract.getAmount() != null ? contract.getAmount().toPlainString() : null);
        return m;
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
        validatePercentageRange("commissionPct", commissionPct);
        validatePercentageRange("listingAgentCommissionPct", listingPct);
        validatePercentageRange("buyerAgentCommissionPct", buyerPct);

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
        if (commissionPct.subtract(sumPct).abs().compareTo(PERCENTAGE_TOLERANCE) > 0) {
            throw new BadRequestException(
                    "commissionPct (" + commissionPct + "%) debe ser igual a la suma de " +
                    "listingAgentCommissionPct (" + listingPct + "%) + " +
                    "buyerAgentCommissionPct (" + buyerPct + "%)");
        }
    }

    private void validatePercentageRange(String fieldName, BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(MAX_PERCENTAGE) > 0) {
            throw new BadRequestException(fieldName + " debe estar entre 0 y 100");
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
        boolean valid;
        if (requester.getRole() == UserRole.AGENT) {
            // Agents drive the full signing workflow, including sending drafts
            valid = switch (current) {
                case DRAFT            -> next == ContractStatus.SENT || next == ContractStatus.CANCELLED;
                case SENT             -> next == ContractStatus.PARTIALLY_SIGNED
                                      || next == ContractStatus.REJECTED
                                      || next == ContractStatus.CANCELLED;
                case PARTIALLY_SIGNED -> next == ContractStatus.SIGNED
                                      || next == ContractStatus.REJECTED
                                      || next == ContractStatus.CANCELLED;
                default               -> false; // SIGNED, REJECTED, EXPIRED, CANCELLED son terminales
            };
        } else {
            // BUYER / SELLER / OWNER: can only sign, reject, or cancel — cannot send a draft
            valid = switch (current) {
                case DRAFT            -> next == ContractStatus.SENT
                                      || next == ContractStatus.CANCELLED;
                case SENT             -> next == ContractStatus.PARTIALLY_SIGNED
                                      || next == ContractStatus.REJECTED
                                      || next == ContractStatus.CANCELLED;
                case PARTIALLY_SIGNED -> next == ContractStatus.SIGNED
                                      || next == ContractStatus.REJECTED
                                      || next == ContractStatus.CANCELLED;
                default               -> false;
            };
        }
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

    private boolean canManageContract(Contract contract, User requester) {
        if (requester.getRole() == UserRole.ADMIN) return true;

        Long uid = requester.getId();
        if (uid.equals(safeId(contract.getSeller()))) return true;

        if (requester.getRole() == UserRole.AGENT) {
            AgentProfile agentProfile = agentProfileRepository.findByUser_Id(uid).orElse(null);
            if (agentProfile != null) {
                Long aid = agentProfile.getId();
                if (aid.equals(safeId(contract.getListingAgent()))) return true;
                if (aid.equals(safeId(contract.getBuyerAgent()))) return true;
            }
        }

        return false;
    }

    private boolean canManagePropertyContracts(Property property, User requester) {
        if (requester.getRole() == UserRole.ADMIN) return true;

        Long uid = requester.getId();
        if (property.getOwner() != null && uid.equals(property.getOwner().getId())) {
            return true;
        }

        if (requester.getRole() == UserRole.AGENT && property.getAgent() != null) {
            AgentProfile agentProfile = agentProfileRepository.findByUser_Id(uid).orElse(null);
            return agentProfile != null && property.getAgent().getId().equals(agentProfile.getId());
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
}
