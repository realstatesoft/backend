package com.openroof.openroof.service;

import com.openroof.openroof.dto.contract.ContractRequest;
import com.openroof.openroof.dto.contract.ContractResponse;
import com.openroof.openroof.dto.contract.ContractSummaryResponse;
import com.openroof.openroof.dto.contract.ContractStatusUpdateRequest;
import com.openroof.openroof.dto.contract.SignContractRequest;
import com.openroof.openroof.exception.BadRequestException;

import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.contract.Contract;
import com.openroof.openroof.model.enums.ContractStatus;
import com.openroof.openroof.model.enums.ContractType;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.ContractRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.repository.ContractSignatureRepository;
import com.openroof.openroof.model.enums.SignatureRole;
import com.openroof.openroof.model.enums.SignatureType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración para ContractService.
 * Prueba la lógica de negocio completa con BD real.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContractServiceIntegrationTest {

    @Autowired
    ContractService contractService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    AgentProfileRepository agentProfileRepository;

    @Autowired
    ContractRepository contractRepository;

    @Autowired
    ContractSignatureRepository contractSignatureRepository;

    @MockitoBean
    EmailService emailService;

    private User agent;
    private User buyer;
    private User seller;
    private AgentProfile agentProfile;
    private Property property;

    @BeforeEach
    void setUp() {
        agent = userRepository.save(User.builder()
                .email("agent@test.com")
                .name("Agent Test")
                .passwordHash("hashed")
                .role(UserRole.AGENT)
                .build());

        buyer = userRepository.save(User.builder()
                .email("buyer@test.com")
                .name("Buyer Test")
                .passwordHash("hashed")
                .role(UserRole.USER)
                .build());

        seller = userRepository.save(User.builder()
                .email("seller@test.com")
                .name("Seller Test")
                .passwordHash("hashed")
                .role(UserRole.USER)
                .build());

        agentProfile = agentProfileRepository.save(AgentProfile.builder()
                .user(agent)
                .build());

        property = propertyRepository.save(Property.builder()
                .title("Test Property")
                .address("123 Test St")
                .price(new BigDecimal("150000"))
                .propertyType(PropertyType.APARTMENT)
                .owner(seller)
                .build());
    }

    @Test
    @DisplayName("Crear contrato directo sin agentes")
    void createDirectContract() {
        ContractRequest request = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                null, null,
                ContractType.SALE,
                new BigDecimal("150000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "Terms", null
        );

        // El vendedor puede crear un contrato donde él es vendedor
        ContractResponse response = contractService.create(request, seller.getEmail());

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.propertyId()).isEqualTo(property.getId());
        assertThat(response.buyerId()).isEqualTo(buyer.getId());
        assertThat(response.sellerId()).isEqualTo(seller.getId());
        assertThat(response.status()).isEqualTo(ContractStatus.DRAFT);
        assertThat(response.commissionPct()).isEqualTo(BigDecimal.ZERO);

        assertThat(contractRepository.findById(response.id())).isPresent();
    }

    @Test
    @DisplayName("El comprador no puede crear un contrato si no gestiona la operación")
    void buyerCannotCreateContract() {
        ContractRequest request = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                null, null,
                ContractType.SALE,
                new BigDecimal("150000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "Terms", null
        );

        assertThatThrownBy(() -> contractService.create(request, buyer.getEmail()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("permiso");
    }

    @Test
    @DisplayName("Crear contrato con agente listador")
    void createWithListingAgent() {
        ContractRequest request = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                agentProfile.getId(), null,
                ContractType.SALE,
                new BigDecimal("200000"),
                new BigDecimal("5"), new BigDecimal("5"), BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "Terms", null
        );

        // El agente listador puede crear el contrato
        ContractResponse response = contractService.create(request, agent.getEmail());

        assertThat(response.listingAgentId()).isEqualTo(agentProfile.getId());
        assertThat(response.commissionPct()).isEqualTo(new BigDecimal("5"));
        assertThat(response.listingAgentCommissionPct()).isEqualTo(new BigDecimal("5"));
    }

    @Test
    @DisplayName("Crear contrato con dual agency")
    void createWithDualAgency() {
        User agent2 = userRepository.save(User.builder()
                .email("agent2@test.com")
                .name("Agent 2")
                .passwordHash("hashed")
                .role(UserRole.AGENT)
                .build());

        AgentProfile agentProfile2 = agentProfileRepository.save(AgentProfile.builder()
                .user(agent2)
                .build());

        ContractRequest request = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                agentProfile.getId(), agentProfile2.getId(),
                ContractType.RENT,
                new BigDecimal("1500"),
                new BigDecimal("10"), new BigDecimal("6"), new BigDecimal("4"),
                LocalDate.now(), LocalDate.now().plusYears(1),
                "Rental", null
        );

        // El agente listador crea el contrato
        ContractResponse response = contractService.create(request, agent.getEmail());

        assertThat(response.listingAgentId()).isEqualTo(agentProfile.getId());
        assertThat(response.buyerAgentId()).isEqualTo(agentProfile2.getId());
        assertThat(response.commissionPct()).isEqualTo(new BigDecimal("10"));
        assertThat(response.listingAgentCommissionPct()).isEqualTo(new BigDecimal("6"));
        assertThat(response.buyerAgentCommissionPct()).isEqualTo(new BigDecimal("4"));
    }

    @Test
    @DisplayName("Validación: comisión total no coincide con suma de partes")
    void createWithInvalidCommissionSum() {
        ContractRequest request = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                agentProfile.getId(), null,
                ContractType.SALE,
                new BigDecimal("150000"),
                new BigDecimal("10"), // Total debe ser 5, no 10
                new BigDecimal("5"), BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "Terms", null
        );

        assertThatThrownBy(() -> contractService.create(request, agent.getEmail()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("commissionPct");
    }

    @Test
    @DisplayName("Validación: comisión > 100%")
    void createWithInvalidPercentage() {
        ContractRequest request = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                agentProfile.getId(), null,
                ContractType.SALE,
                new BigDecimal("150000"),
                new BigDecimal("101"), // > 100%
                new BigDecimal("101"), BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "Terms", null
        );

        assertThatThrownBy(() -> contractService.create(request, agent.getEmail()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("100");
    }

    @Test
    @DisplayName("Validación: agente listador null pero % > 0")
    void createWithListingAgentPercentageButNoAgent() {
        ContractRequest request = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                null, null, // No listing agent
                ContractType.SALE,
                new BigDecimal("150000"),
                new BigDecimal("5"),
                new BigDecimal("5"), // Pero này > 0!!
                BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "Terms", null
        );

        assertThatThrownBy(() -> contractService.create(request, agent.getEmail()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("agente listador");
    }

    @Test
    @DisplayName("Validación: comisión directa (sin agentes, debe ser 0)")
    void createDirectWithCommission() {
        ContractRequest request = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                null, null,
                ContractType.SALE,
                new BigDecimal("150000"),
                new BigDecimal("5"), // Comisión sin agentes!
                BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "Terms", null
        );

        assertThatThrownBy(() -> contractService.create(request, agent.getEmail()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("comisión");
    }

    @Test
    @DisplayName("Obtener contrato como comprador (parte involucrada)")
    void getAsInvolvedBuyer() {
        ContractRequest request = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                null, null,
                ContractType.SALE,
                new BigDecimal("150000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "Terms", null
        );

        ContractResponse created = contractService.create(request, seller.getEmail());
        ContractResponse fetched = contractService.getById(created.id(), buyer.getEmail());

        assertThat(fetched.id()).isEqualTo(created.id());
        assertThat(fetched.buyerId()).isEqualTo(buyer.getId());
    }

    @Test
    @DisplayName("Obtener contrato como usuario no involucrado debe fallar")
    void getAsUnauthorized() {
        ContractRequest request = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                null, null,
                ContractType.SALE,
                new BigDecimal("150000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "Terms", null
        );

        ContractResponse created = contractService.create(request, seller.getEmail());

        User otherUser = userRepository.save(User.builder()
                .email("other@test.com")
                .name("Other")
                .passwordHash("hashed")
                .role(UserRole.USER)
                .build());

        assertThatThrownBy(() -> contractService.getById(created.id(), otherUser.getEmail()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("permiso");
    }

    @Test
    @DisplayName("Listar contratos como agente listador")
    void getAsListingAgent() {
        ContractRequest request = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                agentProfile.getId(), null,
                ContractType.SALE,
                new BigDecimal("200000"),
                new BigDecimal("5"), new BigDecimal("5"), BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "Terms", null
        );

        contractService.create(request, agent.getEmail());

        List<ContractSummaryResponse> contracts = contractService.getAsListingAgent(agent.getEmail());

        assertThat(contracts).isNotEmpty();
        assertThat(contracts.size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Listar contratos como comprador")
    void getAsBuyer() {
        ContractRequest request = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                null, null,
                ContractType.SALE,
                new BigDecimal("150000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "Terms", null
        );

        contractService.create(request, seller.getEmail());

        List<ContractSummaryResponse> contracts = contractService.getAsBuyer(buyer.getEmail());

        assertThat(contracts).isNotEmpty();
    }

    @Test
    @DisplayName("Listar contratos como vendedor")
    void getAsSeller() {
        ContractRequest request = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                null, null,
                ContractType.SALE,
                new BigDecimal("150000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "Terms", null
        );

        contractService.create(request, seller.getEmail());

        List<ContractSummaryResponse> contracts = contractService.getAsSeller(seller.getEmail());

        assertThat(contracts).isNotEmpty();
    }

    @Test
    @DisplayName("Obtener contratos por propiedad devuelve vacío para el propietario cuando aún no existen contratos")
    void getByPropertyAsOwnerWithoutContracts() {
        List<ContractSummaryResponse> contracts = contractService.getByProperty(property.getId(), seller.getEmail());

        assertThat(contracts).isEmpty();
    }

    @Test
    @DisplayName("Cambiar estado DRAFT → SENT")
    void updateStatusDraftToSent() {
        ContractRequest createRequest = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                agentProfile.getId(), null,
                ContractType.SALE,
                new BigDecimal("150000"),
                new BigDecimal("5"), new BigDecimal("5"), BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "Terms", null
        );

        ContractResponse created = contractService.create(createRequest, agent.getEmail());

        ContractStatusUpdateRequest updateRequest = new ContractStatusUpdateRequest(ContractStatus.SENT);
        ContractResponse updated = contractService.updateStatus(created.id(), updateRequest, agent.getEmail());

        assertThat(updated.status()).isEqualTo(ContractStatus.SENT);
    }

    @Test
    @DisplayName("Firmar contrato como comprador y verificar transición a PARTIALLY_SIGNED")
    void signAsBuyer() {
        // 1. Preparar contrato en estado SENT
        ContractRequest createRequest = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                null, null,
                ContractType.SALE,
                new BigDecimal("150000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "Terms", null
        );
        ContractResponse created = contractService.create(createRequest, seller.getEmail());
        contractService.updateStatus(created.id(), new ContractStatusUpdateRequest(ContractStatus.SENT), seller.getEmail());

        // 2. Firmar como comprador
        SignContractRequest signRequest = new SignContractRequest(
                SignatureType.DIGITAL, SignatureRole.BUYER, "hash-evidencia"
        );
        ContractResponse signedRes = contractService.sign(created.id(), signRequest, buyer.getEmail(), "127.0.0.1");

        // 3. Verificar
        assertThat(signedRes.status()).isEqualTo(ContractStatus.PARTIALLY_SIGNED);
        assertThat(contractSignatureRepository.countByContractIdAndSignedAtIsNotNullAndDeletedAtIsNull(created.id())).isEqualTo(1);
    }

    @Test
    @DisplayName("Flujo completo de firmas: transición automática a SIGNED")
    void fullSignatureFlow() {
        // 1. Crear contrato (no tiene agentes, solo requiere 2 firmas: comprador y vendedor)
        ContractRequest createRequest = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                null, null,
                ContractType.SALE,
                new BigDecimal("150000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "Terms", null
        );
        ContractResponse created = contractService.create(createRequest, seller.getEmail());
        contractService.updateStatus(created.id(), new ContractStatusUpdateRequest(ContractStatus.SENT), seller.getEmail());

        // 2. Firma el Comprador
        contractService.sign(created.id(), 
            new SignContractRequest(SignatureType.DIGITAL, SignatureRole.BUYER, "sig-buyer"), 
            buyer.getEmail(), "127.0.0.1");

        // 3. Firma el Vendedor
        ContractResponse finalRes = contractService.sign(created.id(), 
            new SignContractRequest(SignatureType.DIGITAL, SignatureRole.SELLER, "sig-seller"), 
            seller.getEmail(), "127.0.0.1");

        // 4. Verificación final: El contrato DEBE estar SIGNED
        assertThat(finalRes.status()).isEqualTo(ContractStatus.SIGNED);
    }

    @Test
    @DisplayName("Error al firmar dos veces con el mismo rol")
    void signDoubleFails() {
        ContractRequest createRequest = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                null, null,
                ContractType.SALE,
                new BigDecimal("150000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "Terms", null
        );
        ContractResponse created = contractService.create(createRequest, seller.getEmail());
        contractService.updateStatus(created.id(), new ContractStatusUpdateRequest(ContractStatus.SENT), seller.getEmail());

        SignContractRequest signRequest = new SignContractRequest(SignatureType.DIGITAL, SignatureRole.BUYER, "sig1");
        contractService.sign(created.id(), signRequest, buyer.getEmail(), "127.0.0.1");

        // Intentar firmar otra vez debe fallar
        assertThatThrownBy(() -> contractService.sign(created.id(), signRequest, buyer.getEmail(), "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ya ha firmado");
    }

    @Test
    @DisplayName("Error al firmar con un rol que no corresponde al usuario")
    void signUnauthorizedRole() {
        ContractRequest createRequest = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                null, null,
                ContractType.SALE,
                new BigDecimal("150000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "Terms", null
        );
        ContractResponse created = contractService.create(createRequest, seller.getEmail());
        contractService.updateStatus(created.id(), new ContractStatusUpdateRequest(ContractStatus.SENT), seller.getEmail());

        // El COMPRADOR intenta firmar como VENDEDOR
        SignContractRequest signRequest = new SignContractRequest(SignatureType.DIGITAL, SignatureRole.SELLER, "hack");
        
        assertThatThrownBy(() -> contractService.sign(created.id(), signRequest, buyer.getEmail(), "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no corresponde");
    }

    @Test
    @DisplayName("Eliminar contrato (soft delete - reclama admin)")
    void deleteContract() {
        ContractRequest createRequest = new ContractRequest(
                property.getId(), buyer.getId(), seller.getId(),
                null, null,
                ContractType.SALE,
                new BigDecimal("150000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "Terms", null
        );

        ContractResponse created = contractService.create(createRequest, seller.getEmail());
        Long contractId = created.id();

        User admin = userRepository.save(User.builder()
                .email("admin@test.com")
                .name("Admin")
                .passwordHash("hashed")
                .role(UserRole.ADMIN)
                .build());

        // Eliminar
        contractService.delete(contractId, admin.getEmail());

        // Después de eliminar (soft delete), debería tener deletedAt asignado
        Contract deletedContract = contractRepository.findById(contractId).orElseThrow();
        assertThat(deletedContract.getDeletedAt()).isNotNull();
    }
}

